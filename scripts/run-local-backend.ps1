# Runs the backend against a private PostgreSQL in Docker, for trying out features whose
# migrations are not yet on the shared database. Everything else, such as the Gemini key under
# app.ai.assistant, comes from your own application-local.yml as in a normal run. Never touches
# Supabase or application-local.yml: the datasource and demo-seed settings below are process
# environment variables, which take precedence over application-local.yml for this run only.
#
#   powershell -ExecutionPolicy Bypass -File scripts\run-local-backend.ps1
#
# It serves on port 8080; when another backend is already there, set a different port first:
#   $env:SERVER_PORT = '8081'
#
# The database keeps its data between runs. To start again from empty:
#   docker rm -f studybuddy-local-db
# Demo accounts are seeded on every run (only missing ones are added): priya@demo.example.test and
# the others in the README, with the password below. Stop the backend with Ctrl+C.

# Failures are checked through $LASTEXITCODE: PowerShell 5.1 would treat a program's expected
# stderr output, such as "no such container" on the first run, as fatal under 'Stop'.
$Container = 'studybuddy-local-db'
$Port = 55433
# Local-only throwaway credentials for a database bound to 127.0.0.1; not used anywhere shared.
$DatabasePassword = 'local-only-password'
$DemoPassword = 'demo-password-1'
$RepoRoot = Split-Path -Parent $PSScriptRoot
# PowerShell 5.1 pipes text to programs as ASCII by default, which would mangle non-ASCII SQL.
$OutputEncoding = New-Object System.Text.UTF8Encoding $false

function Invoke-Psql([string[]] $Arguments) {
    docker exec -i $Container psql -h 127.0.0.1 -U studybuddy -d studybuddy --no-psqlrc @Arguments
}

docker container inspect $Container *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Creating local database $Container on 127.0.0.1:$Port"
    docker run --detach --name $Container --label com.studybuddy.purpose=local `
        --publish "127.0.0.1:${Port}:5432" `
        -e POSTGRES_DB=studybuddy -e POSTGRES_USER=studybuddy -e "POSTGRES_PASSWORD=$DatabasePassword" `
        postgres:17 | Out-Null
} else {
    docker start $Container | Out-Null
}

Write-Host 'Waiting for PostgreSQL...'
$ready = $false
for ($attempt = 0; $attempt -lt 60; $attempt++) {
    # TCP inside the container answers only once the final server, not the init server, is up.
    Invoke-Psql @('-c', 'SELECT 1') *> $null
    if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    Start-Sleep -Seconds 1
}
if (-not $ready) { throw 'PostgreSQL did not become ready' }

Write-Host 'Applying migrations...'
$migrations = Get-ChildItem "$RepoRoot\backend\src\main\resources\db\migrations\*.sql" | Sort-Object Name
$sql = "SET LOCAL client_min_messages=warning;`n" + (($migrations | ForEach-Object { Get-Content $_.FullName -Raw -Encoding UTF8 }) -join "`n")
# Piped straight to docker: text piped into a PowerShell function does not reach a program inside it.
$sql | docker exec -i $Container psql -h 127.0.0.1 -U studybuddy -d studybuddy --no-psqlrc `
    --quiet --set=ON_ERROR_STOP=1 --single-transaction | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'A migration failed' }
$planTable = Invoke-Psql @('--tuples-only', '--no-align', '-c', "SELECT to_regclass('public.study_plans') IS NOT NULL")
if ("$planTable".Trim() -ne 't') { throw 'Migrations did not create the expected tables' }

$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://127.0.0.1:$Port/studybuddy"
$env:SPRING_DATASOURCE_USERNAME = 'studybuddy'
$env:SPRING_DATASOURCE_PASSWORD = $DatabasePassword
$env:DEMO_SEED_ENABLED = 'true'
$env:DEMO_STUDENT_PASSWORD = $DemoPassword
$env:DEMO_ADMIN_PASSWORD = $DemoPassword

$serverPort = if ([string]::IsNullOrWhiteSpace($env:SERVER_PORT)) { '8080' } else { $env:SERVER_PORT }
Write-Host "Starting the backend on http://localhost:$serverPort/api against the local database"
Write-Host 'The AI assistant uses app.ai.assistant in your application-local.yml (off unless enabled there)'
Write-Host "Demo sign-in: priya@demo.example.test / $DemoPassword"
Push-Location "$RepoRoot\backend"
try {
    .\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
    # A later normal run in this window goes back to the datasource in application-local.yml.
    Remove-Item Env:SPRING_DATASOURCE_URL, Env:SPRING_DATASOURCE_USERNAME, Env:SPRING_DATASOURCE_PASSWORD,
        Env:DEMO_SEED_ENABLED, Env:DEMO_STUDENT_PASSWORD, Env:DEMO_ADMIN_PASSWORD -ErrorAction SilentlyContinue
}
