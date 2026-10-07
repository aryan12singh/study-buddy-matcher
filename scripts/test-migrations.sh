#!/usr/bin/env bash
set -euo pipefail

# The caller creates this empty, disposable database; never use shared project data.
if [ "${PGDATABASE:-}" != studybuddy_migration_test ]; then
  printf 'Migration tests require the disposable studybuddy_migration_test database.\n' >&2
  exit 1
fi
task_repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
task_psql=(psql --no-psqlrc --quiet --set=ON_ERROR_STOP=1)
if [ "$("${task_psql[@]}" --tuples-only --no-align --command="SELECT count(*) FROM pg_tables WHERE schemaname='public'")" != 0 ]; then
  printf 'Migration tests require an empty public schema.\n' >&2
  exit 1
fi
"${task_psql[@]}" --file="$task_repo_root/backend/src/main/resources/db/migrations/20261006170044_studybuddy_baseline.sql"
"${task_psql[@]}" --file="$task_repo_root/scripts/sql/legacy-migration-fixture.sql"

# Exact-case uniqueness permits this legacy collision. The new preflight must abort,
# leaving the whole upgrade and the existing identities/timestamps untouched.
"${task_psql[@]}" --command="INSERT INTO users(id,active,created_at,email,password_hash,role) VALUES (99,true,'2026-10-01 09:15:00','legacy@example.test','fixture-only-placeholder','STUDENT')"
task_failure_log="$(mktemp)"
trap 'rm -f "$task_failure_log"' EXIT
if "$task_repo_root/scripts/migrate-database.sh" > "$task_failure_log" 2>&1; then
  printf 'Invalid duplicate-email upgrade unexpectedly succeeded.\n' >&2
  exit 1
fi
if [[ "$(<"$task_failure_log")" != *"Duplicate normalised emails exist"* ]]; then
  printf 'The migration failed for an unexpected reason.\n' >&2
  exit 1
fi
"${task_psql[@]}" <<'SQL'
DO $$ BEGIN
  IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='users' AND column_name='token_version')
     OR (SELECT data_type FROM information_schema.columns WHERE table_schema='public' AND table_name='users' AND column_name='created_at') <> 'timestamp without time zone'
     OR (SELECT email FROM users WHERE id=10) <> ' Legacy@Example.Test ' THEN
    RAISE EXCEPTION 'Failed preflight partially changed legacy data/schema';
  END IF;
END $$;
DELETE FROM users WHERE id=99;
SQL

"$task_repo_root/scripts/migrate-database.sh"
"${task_psql[@]}" --file="$task_repo_root/scripts/sql/assert-migrated-legacy.sql"
"$task_repo_root/scripts/migrate-database.sh"
"${task_psql[@]}" --file="$task_repo_root/scripts/sql/assert-migrated-legacy.sql"
printf 'Legacy upgrade, preflight rollback and repeat migration checks passed.\n'
