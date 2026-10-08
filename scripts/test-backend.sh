#!/usr/bin/env bash
set -euo pipefail

# Always create a private, disposable PostgreSQL 17 instance for the destructive test suite.
task_repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
task_container="studybuddy-tests-$(date +%s)-$$"
task_database_password="$(openssl rand -hex 24)"
task_jwt_secret="$(openssl rand -base64 48)"
task_env_file="$(mktemp)"
chmod 600 "$task_env_file"
printf 'POSTGRES_DB=studybuddy_test\nPOSTGRES_USER=studybuddy_test\nPOSTGRES_PASSWORD=%s\n' "$task_database_password" > "$task_env_file"

cleanup() {
  docker rm -f "$task_container" >/dev/null 2>&1 || true
  rm -f "$task_env_file"
}
trap cleanup EXIT

docker run --detach --rm --name "$task_container" \
  --label com.studybuddy.purpose=tests \
  --publish 127.0.0.1::5432 --env-file "$task_env_file" postgres:17 >/dev/null
task_ready=false
for task_attempt in {1..60}; do
  if docker exec "$task_container" pg_isready -U studybuddy_test -d studybuddy_test >/dev/null 2>&1; then
    task_ready=true
    break
  fi
  sleep 1
done
if [ "$task_ready" != true ]; then
  printf 'PostgreSQL did not become ready.\n' >&2
  exit 1
fi
task_port="$(docker port "$task_container" 5432/tcp | head -n 1 | awk -F: '{print $NF}')"
export PGHOST=127.0.0.1 PGPORT="$task_port" PGDATABASE=studybuddy_test PGUSER=studybuddy_test PGPASSWORD="$task_database_password"
psql --no-psqlrc --quiet --set=ON_ERROR_STOP=1 --command='CREATE DATABASE studybuddy_migration_test'
PGDATABASE=studybuddy_migration_test "$task_repo_root/scripts/test-migrations.sh"
"$task_repo_root/scripts/migrate-database.sh"
export SPRING_PROFILES_ACTIVE=test
export STUDYBUDDY_TEST_DATABASE_URL="jdbc:postgresql://127.0.0.1:$task_port/studybuddy_test"
export STUDYBUDDY_TEST_DATABASE_USERNAME=studybuddy_test
export STUDYBUDDY_TEST_DATABASE_PASSWORD="$task_database_password"
export STUDYBUDDY_TEST_JWT_SECRET="$task_jwt_secret"
cd "$task_repo_root/backend"
./mvnw -B test "$@"
