#!/usr/bin/env bash
set -euo pipefail

# Connection comes only from libpq environment variables; credentials never enter arguments.
: "${PGHOST:?Set PGHOST}"
: "${PGDATABASE:?Set PGDATABASE}"
: "${PGUSER:?Set PGUSER}"
: "${PGPASSWORD:?Set PGPASSWORD}"

task_repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
task_migration_dir="$task_repo_root/backend/src/main/resources/db/migrations"
task_sql_input="$(mktemp)"
trap 'rm -f "$task_sql_input"' EXIT

# Single connection/transaction prevents a partial upgrade and serializes migration runs.
# Use the same namespace as the application's account-lifecycle guard.
printf 'SET LOCAL client_min_messages=warning;\nDO $$ BEGIN PERFORM pg_advisory_xact_lock(4422026); END $$;\n' > "$task_sql_input"
for task_file in "$task_migration_dir"/*.sql; do
  if command -v cygpath >/dev/null 2>&1; then
    task_file_path="$(cygpath -m "$task_file")"
  else
    task_file_path="$task_file"
  fi
  task_quoted_path="${task_file_path//\'/\'\'}"
  printf "\\\\i '%s'\n" "$task_quoted_path" >> "$task_sql_input"
done
psql --no-psqlrc --quiet --set=ON_ERROR_STOP=1 --single-transaction --file="$task_sql_input"
printf 'Database migrations applied successfully.\n'
