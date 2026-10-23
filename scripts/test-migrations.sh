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
  cat "$task_failure_log" >&2
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
# Populated extension state must survive the repeat, not just an empty new schema.
"${task_psql[@]}" <<'SQL'
INSERT INTO study_rooms(id,study_group_id,version,focus_minutes,break_minutes,participant_limit,
 phase,timer_status,remaining_millis,audio_preset,audio_playing,music_preset,music_playing,ambient_preset,ambient_playing)
 VALUES(90,20,3,25,5,2,'FOCUS','IDLE',1500000,'WHITE_NOISE',true,'CALM_MUSIC',false,'RAIN',true);
INSERT INTO study_sessions(id,study_group_id,created_by,title,start_local,end_local,start_at,end_at,time_zone,
 occurrence_count,status,created_at,updated_at)
 VALUES(91,20,10,'Retained weekly session','2026-10-20 09:00','2026-10-20 10:00',
 '2026-10-20 01:00+00','2026-10-20 02:00+00','Asia/Singapore',2,'SCHEDULED',
 '2026-10-01 01:15+00','2026-10-01 01:15+00');
INSERT INTO notifications(created_at,message,read,type,recipient_id,resource_type,resource_id,event_key) VALUES
 ('2026-10-01 01:15+00','Retained cancellation',false,'MATCH_REQUEST_CANCELLED',10,'MATCH_REQUEST',40,'fixture:cancelled'),
 ('2026-10-01 01:15+00','Retained session event',false,'GROUP_SESSION_CREATED',11,'SESSION',91,'fixture:session'),
 ('2026-10-01 01:15+00','Retained member left',false,'GROUP_MEMBER_LEFT',10,'GROUP',20,'fixture:left'),
 ('2026-10-01 01:15+00','Retained withdrawal',false,'GROUP_JOIN_REQUEST_WITHDRAWN',10,'GROUP',20,'fixture:withdrawn'),
 ('2026-10-01 01:15+00','Legacy withdrawal recorded as rejected',false,'GROUP_JOIN_REQUEST_REJECTED',10,'GROUP',20,'group-request:93:withdrawn');
-- A withdrawal recorded as REJECTED before WITHDRAWN existed is converted once; a real WITHDRAWN row is kept.
INSERT INTO group_join_requests(id,created_at,responded_at,status,student_id,study_group_id) VALUES
 (93,'2026-10-01 01:15+00','2026-10-01 01:15+00','REJECTED',11,20),
 (94,'2026-10-01 01:15+00','2026-10-01 01:15+00','WITHDRAWN',11,20);
-- Reproduce a database that previously ran both Hibernate update and the old migration.
-- Replay must remove its legacy duplicate without deleting the retained cache row.
ALTER TABLE match_explanations ADD CONSTRAINT uk37l18h6kex7l6fcxxjpv5g7jq UNIQUE (evidence_hash);
SQL
"$task_repo_root/scripts/migrate-database.sh"
"${task_psql[@]}" --file="$task_repo_root/scripts/sql/assert-migrated-legacy.sql"
"${task_psql[@]}" <<'SQL'
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM study_rooms WHERE id=90 AND version=3 AND music_preset='CALM_MUSIC'
   AND NOT music_playing AND ambient_preset='RAIN' AND ambient_playing)
  OR NOT EXISTS(SELECT 1 FROM study_sessions WHERE id=91 AND title='Retained weekly session'
   AND occurrence_count=2 AND calendar_uid IS NOT NULL)
  OR (SELECT count(*) FROM notifications WHERE event_key IN ('fixture:cancelled','fixture:session','fixture:left','fixture:withdrawn')) <> 4
  OR (SELECT status FROM group_join_requests WHERE id=93) <> 'WITHDRAWN'
  OR (SELECT status FROM group_join_requests WHERE id=94) <> 'WITHDRAWN'
  OR (SELECT type FROM notifications WHERE event_key='group-request:93:withdrawn') <> 'GROUP_JOIN_REQUEST_WITHDRAWN'
  OR NOT (SELECT convalidated FROM pg_constraint WHERE conrelid='group_join_requests'::regclass AND conname='group_join_requests_status_check')
  OR NOT (SELECT convalidated FROM pg_constraint WHERE conrelid='notifications'::regclass AND conname='notifications_type_check') THEN
   RAISE EXCEPTION 'Populated repeat changed audio/session/event data or left the final enum unvalidated';
 END IF;
END $$;
SQL
printf 'Legacy upgrade, preflight rollback and repeat migration checks passed.\n'
"${task_psql[@]}" --file="$task_repo_root/scripts/sql/test-reset-demo-data.sql"
printf 'Mixed demo reset and repeat-reset preservation checks passed.\n'
