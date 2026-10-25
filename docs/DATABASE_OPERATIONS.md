# Database operations

The application uses backend JDBC with application JWTs. It does not use a browser Supabase
SDK or Supabase Auth policies. This branch's complete migration set defines 20 mapped public
application tables: the 17 baseline tables plus `study_rooms`, `room_presences` and
`study_sessions`, together with their sequences/default privileges and
browser grants; provider-managed schemas are untouched. This describes the schema after
applying the complete ordered migration set. The shared project received the full set on
16 October 2026; see the [E2 rollout record](#e2-rollout--16-october-2026).

## Ordered migrations

| Local SQL file | Purpose |
| --- | --- |
| `20261006170044_studybuddy_baseline.sql` | Existing mapped schema; CREATE IF NOT EXISTS for fresh or matching databases |
| `20261006170327_team_c_integrity.sql` | Preflight, normalized identity, JWT version/last login, request context, safe event metadata, description width, UTC conversion, checks/unique/query indexes |
| `20261006170448_backend_only_database_access.sql` | RLS and deny browser table/sequence grants; future default privileges follow the same model |
| `20261006172934_collection_goal_primary_keys.sql` | Natural owner/goal primary keys for the two set collections; preserve valid rows and reject legacy null goals |
| `20261007220000_match_request_cancellation.sql` | Distinguishes lifecycle cancellation from decline in request and notification constraints |
| `20261009010000_study_rooms.sql` | Private room/timer state and expiring presence leases; backend-only tables/sequences |
| `20261011090000_matching_weights_per_strategy.sql` | Matching weights belong to a strategy: adds `matching_configs.strategy` (existing rows become Balanced) and replaces one weight per criterion with one per strategy and criterion |
| `20261013090000_complete_matching_configuration.sql` | Singleton matching settings, minimum score, persistent revision/override state and `matching_enabled_criteria`; preserves valid legacy overrides and denies browser access to the new table |
| `20261014010000_room_audio_layers.sql` | Independent music/ambient columns and checks; backfills legacy selections once and preserves changed layer state on repetition. Unused since room audio was removed on 24 October; kept so the ordered history still applies |
| `20261014020000_study_sessions.sql` | Private group/buddy dated sessions, bounded schedule/parent/status checks, calendar UID and scoped query indexes; backend-only table/sequence |

These are versioned source files. The historical shared
MCP migration runner records its own deployment timestamps, so do not expect those history
versions to equal local filename prefixes. Match history by migration name. Do not modify
an applied file to change a deployed schema: add another migration.

`scripts/migrate-database.sh` wraps all files in one transaction with ON_ERROR_STOP and the
exclusive application advisory lock. It is intentionally repeatable, including existing-schema
adoption.

### Deployment record and PR #54 rollout

The 7 October 2026 hosted record below covers the Team C baseline/integrity/access work
and seed verification at that time. It does not establish that subsequently added matching
migrations have run on the shared project. The original 13 October audit used only
disposable/local PostgreSQL for application testing and did not alter shared Supabase data.
The later compulsory rollout is recorded separately below.

At the original 13 October merged-main audit, the complete matching configuration migration
had not been applied to shared Supabase. The dated compulsory rollout below resolves that
gate. Future fresh targets and upgrades still require the complete ordered set, verification
of settings columns, enabled criteria, constraints and grants. Do not rely on Hibernate
`update` to supply the singleton, access controls or revision adoption.

On 13 October, local verification passed fresh/repeated migrations, synthetic legacy
upgrade, rejected-preflight rollback, mapping validation and browser-role denial tests.
An actual Java process restart preserved saved weights, enabled criteria, minimum score,
revision and matching output. Detailed application evidence is in
[TEAM_A_TESTING.md](TEAM_A_TESTING.md#verification-record---13-october-2026).

### Compulsory rollout — 13 October 2026

The shared project `tagmkcbjespzgljdafiv` was checked again before this completion pass.
It had 10 courses, 52 student profiles and 53 accounts. A partial mapped update had created
`matching_enabled_criteria`, but the settings columns and that table's RLS were still missing.
Browser SELECT grants remained denied. The existing migration history alone did not describe
this partial state, so schema metadata was checked directly.

Before the rollout, a private PostgreSQL 17 custom-format backup of the public application
schema/data was captured outside tracked source. It was restored into an isolated database,
including the provider roles needed for ACL restoration. The complete ordered SQL set passed
against that restored database, retaining identical account and student-profile data.

The seven compulsory source migrations (excluding the later E2 room/audio/session files)
were then applied transactionally through the hosted
migration runner, with the exclusive application advisory lock and bounded lock/statement
timeouts. History records the operation as `merged_main_compulsory_schema_rollout`.
No application table was truncated, seeded or reset; no existing account was deleted.

Post-rollout checks confirmed:

- 10 courses, 52 student profiles and 53 accounts remained.
- `matching_strategy_settings` includes `min_score`, `config_version` and `has_override`,
  with one valid singleton row; five enabled criteria were present.
- All 17 mapped public application tables have RLS enabled and deny browser-role SELECT.
  The new enabled-criteria table follows the same backend-only access model.
- Request/notification cancellation constraints and per-strategy matching constraints are
  installed with the ordered set.
- The packaged Java 21 application started against the actual shared database with
  `ddl-auto=validate` and demo seeding disabled. This was a temporary schema/wiring check,
  not a claim that the feature branch was deployed as a persistent hosted application.

Security advisors report only the 17 informational no-policy notices expected for this
backend-only architecture. The browser has no direct table grants; adding permissive policies
is not required. See the [advisor explanation](https://supabase.com/docs/guides/database/database-linter?lint=0008_rls_enabled_no_policy)
and [RLS/grants guidance](https://supabase.com/docs/guides/database/postgres/row-level-security).
The private backup stays outside the repository. It must not be attached to a PR or committed.
Reproducible application checks are in [TEAM_C_TESTING.md](TEAM_C_TESTING.md).

### Matching schema and adoption

The 17 application tables are `courses`, `users`, `students`, `student_courses`,
`student_study_goals`, `availability_slots`, `match_requests`, `connections`, `study_groups`,
`study_group_goals`, `group_availability_slots`, `group_memberships`, `group_join_requests`,
`notifications`, `matching_configs`, `matching_strategy_settings` and
`matching_enabled_criteria`.

`matching_strategy_settings` has one row with `id=1`; its current fields include
`active_strategy`, `updated_at`, `min_score` (API `minScore`, 0-1), `config_version`
(non-negative persistent revision), and `has_override` (stored values versus external
startup defaults). `matching_enabled_criteria` stores the enabled criteria with natural
primary key `(setting_id,criterion)`, a foreign key to that singleton and the five allowed
criterion names. `matching_configs` remains unique per `(strategy,criterion)`.

The new migration rejects multiple legacy settings rows instead of silently choosing one.
A valid single legacy row is normalized to ID 1, retains its strategy/weights, starts at
revision 1 and adopts all five enabled criteria. Fresh data with no override rows starts at
revision 0 and uses external startup defaults. Existing weight-only overrides are retained.
Repeated execution retains saved revisions, thresholds, override flags and explicit enabled
choices; it does not reset them or re-enable a deliberately disabled criterion. Restore
clears overrides through the application while preserving the increasing revision. The new
table enables RLS and revokes `PUBLIC`/`anon`/`authenticated` access in the same migration.

## Before applying to a populated database

1. Check the exact target/owner and confirm no conflicting application version is writing.
2. Take a provider snapshot or a PostgreSQL 17 `pg_dump` of application schema/data, including
   sequence state and grants/policies; store it privately outside tracked source. A PostgreSQL
   14 pg_dump cannot dump PostgreSQL 17. Test restore into an isolated database.
3. Read the preflight errors. Normalized email collisions, duplicate pending unordered
   buddy pairs, duplicate active connection pairs, duplicate pending applicants, self pairs,
   invalid years/capacity/slots, over-capacity membership and null collection goals must be
   resolved explicitly. No migration deletes rows to make a constraint pass.
4. Confirm the legacy event timezone. This project's LocalDateTime records were campus-local
   Asia/Singapore; the conversion uses `AT TIME ZONE 'Asia/Singapore'` only while the column
   is still timestamp-without-time-zone. Changing timezone assumptions needs a reviewed
   migration, not a runtime flag.
5. Apply files in order and verify mappings, constraints and access. The app and tests run
   with `ddl-auto: validate`, so Hibernate never alters a database and startup stops, naming
   it, when a mapped table or column is missing. Validation does not check constraints,
   indexes, primary keys or row-level security, so every migration is still required.

The historical 7 October shared upgrade preflight found the mapped database empty. An
ignored private schema snapshot (`.env.shared-schema-backup.json`) captured columns, constraints, indexes, grants,
RLS, policies and counts before changes; there were no application rows to export. No shared
table was reset, truncated or deleted. Destructive tests use disposable local PostgreSQL only;
see [Testing](../README.md#testing).

## Local fresh database or reviewed existing upgrade

Supply libpq connection variables privately. Use the owner/backend role, not anon/authenticated.

```bash
export PGHOST=<host> PGPORT=5432 PGDATABASE=<database> PGUSER=<owner>
# Set PGPASSWORD privately in this shell; the runner requires it.
scripts/migrate-database.sh
scripts/migrate-database.sh  # repeatability check, not a data reset
```

The automatic isolated test runner creates a PostgreSQL 17 container with random private
credentials and a random localhost port, applies the SQL, runs Maven and removes only its
own container/config. It never reads application-local.yml to choose the test database.
Before the application suite, it also checks a synthetic legacy schema and rejected-preflight
rollback in the separate empty `studybuddy_migration_test` database.

## Safe demo seed

Add runtime `DEMO_SEED_ENABLED=true`, `DEMO_STUDENT_PASSWORD` and `DEMO_ADMIN_PASSWORD`,
or corresponding `app.demo-seed` keys in ignored backend config. Both passwords obey the
normal eight-character / 72-UTF-8-byte rules. Run backend once and then disable seeding.

The transactional seed takes the exclusive write guard and reads
`backend/src/main/resources/demo-seed/demo-data.json`. It adds missing course codes and
identities: 14 courses, 50 synthetic students and one admin. Students are created through
`AccountCreation` and `ProfileService`, so the normal validation applies.

When **none** of the 50 demo students existed beforehand, it then replays the demo history
through the real services: match requests, connections (one ended), four groups (one full,
one closed) and one administrator deactivation. Notifications are produced exactly as in live
use. This is the only case where it writes relations.

Existing course names, profiles and password hashes are preserved. It neither resets nor
repairs existing user data, and does not reseed relations or reactivate an existing identity.
If an operator deletes a seeded identity, a later explicitly enabled seed can recreate it as a
new identity; leave seeding disabled during normal account-lifecycle demonstrations.

**Moving a shared database from the earlier seed.** A database seeded before October 2026 still
holds the old `student04`–`student50` identities. The new seed adds its students alongside them
but skips the history, because `priya`, `jamie` and `alex` already exist. To get the full demo
state, an approved operator does a one-off reset:

1. **Stop every backend** pointed at the database, and tell the team. Anything done with demo
   accounts (requests, groups, edits) will be lost.
2. **Run the whole of [`scripts/sql/reset-demo-data.sql`](../scripts/sql/reset-demo-data.sql)**
   once, in the Supabase SQL editor or with `psql --set=ON_ERROR_STOP=1 --file=...`. Run the
   full file, not a selection.
   - It deletes every `@demo.example.test` account and everything depending on it in a single
     `DO` statement, so it is atomic however the client sends it: an error changes nothing. It
     ends by printing `demo_accounts_remaining = 0`.
   - An earlier version kept its working lists in temporary tables across several statements and
     failed in the Supabase SQL editor with `relation "demo_students" does not exist`, before any
     delete ran. If you saw that error, nothing was removed; run the current file.
   - Courses, matching configuration and non-demo accounts are kept. A non-demo account only
     loses its own requests, connections, memberships and applications that involved demo
     accounts, plus notifications targeting the removed resources. The reset captures
     affected group/buddy session IDs before parent cascades, then deletes only their
     `SESSION` notifications. Unrelated notices and sessions under surviving parents are
     retained, including a retained session originally created by a demo student.
3. **Start one backend with seeding enabled**, then **wait for the seed to finish** before
   stopping it: the seed runs after startup and is rolled back if the app stops mid-way. It is
   complete when `select count(*) from study_groups where name = 'IS212 Sprint Retro'` returns 1.
4. **Disable seeding** again and sign in as `priya@demo.example.test` to check the demo state.

The full sequence (old seed with a non-demo account interacting with demo accounts, then the
reset script, then the new seed) was rehearsed on a disposable local PostgreSQL. It produced
14 courses, 50 demo students plus the untouched non-demo account, one admin, one deactivated
student and the four demo groups.
<!-- REVIEW (Team B): agree who performs the shared-database reset and when, before running it. -->

Handles include priya, jamie, alex, appendix.a, appendix.b, hannah, ethan, ryan and admin, all
under `@demo.example.test`; the full list is in `demo-data.json`. The operator privately supplies passwords. On this machine,
`.env.demo.local` holds the shared demo passwords; source it only for intentional seed use.
Never paste it into a frontend env file, commit it or attach it to a PR.

Verify counts and existing-data preservation through authorized SQL. Compare a checksum
of identity/password/creation fields before/after repeat without printing hashes. App startup
messages precede ApplicationRunner completion; wait for a seed count check before claiming
the seed finished. The historical 7 October shared run recorded 10 courses, 50 students,
51 users and a second startup without identity/password/creation changes. These counts
are historical evidence, not a fresh hosted count for the current branch.

### E2 rollout — 16 October 2026

PR #52 (E2 collaborative study) merged to `main` as `66d121a`. Before anything ran against
shared Supabase, the shared project `tagmkcbjespzgljdafiv` was checked directly. It still had
the 18 pre-E2 public tables (the 17 mapped baseline tables plus an unmapped, empty,
RLS-protected `match_explanations` table from the separate E1 branch), with 53 accounts,
52 student profiles, 4 groups, 26 notifications and 3 connections. Its
`notifications_type_check` did not yet allow session notification types.

Dry run first. A schema-only copy of the shared `public` schema was restored into a
disposable PostgreSQL 17 database with the provider roles created for ACL restoration. The
complete ordered SQL set from the merged source applied successfully twice (proving it is
safe to repeat), and the packaged Java 21 application then started against the copy with
`ddl-auto=validate`. The disposable database was deleted afterwards.

Rollout. A private PostgreSQL 17 custom-format backup of the shared `public` schema and data
was captured outside tracked source and test-restored into an isolated database, where its
row counts matched the live project exactly. `scripts/migrate-database.sh` from `66d121a`
was then run through the project's IPv4 session pooler (the direct database host is
IPv6-only), as one transaction under the exclusive application advisory lock with bounded
lock and statement timeouts. Because this used the repository runner rather than the hosted
MCP runner, no MCP migration-history entry exists for this rollout; this section is the record.
No application table was truncated, seeded or reset.

Post-rollout checks confirmed:

- 21 public tables: the 20 mapped application tables plus the unmapped E1 table above.
- `study_rooms`, `room_presences` and `study_sessions` have RLS enabled and deny anon and
  authenticated SELECT, with 11, 2 and 5 check constraints respectively, including
  `occurrence_count` between 1 and 52.
- `notifications_type_check` includes the six group/buddy session types and is validated.
- No public table is readable by anon or authenticated, and none has RLS disabled.
- 53 accounts, 52 student profiles, 4 groups, 26 notifications and 3 connections remained.
- The packaged application from `66d121a` started against the shared database with
  `ddl-auto=validate`, demo seeding and live weather disabled. This was a temporary
  schema/wiring check, not a persistent hosted deployment.

Restore path if needed: the backup is a `pg_restore`-compatible custom-format dump kept
privately by the operator. It must not be attached to a PR or committed.

### Withdraw and leave rollout — 23 October 2026

PR #67 adds `20261023090000_withdraw_and_leave.sql`. It was applied to the shared project
`tagmkcbjespzgljdafiv` before the PR merged, because the code needs the new values and the
current code does not yet write them. No old withdrawals existed to relabel, so no row changed.

Backup first. The Supabase CLI (2.117.0, signed in to the project owner's account, using its
temporary login role rather than a database password) dumped the roles, the schema and the
data, including Supabase's `auth` tables. The files are kept privately by the operator outside
tracked source and must not be attached to a PR or committed. The `public` part was
test-restored into a disposable PostgreSQL 17 database, where all 21 tables matched the dump
row for row: 55 users, 54 students, 4 groups, 10 group applications, 12 memberships,
7 buddy requests, 4 connections and 31 notifications.

Dry run. On that restored copy, a preview of the backfill found no `REJECTED` application with
a `:withdrawn` event key and no withdrawal notification to relabel. `scripts/migrate-database.sh`
from the PR branch (`9bd9264`) then applied the complete ordered set twice, and the packaged
application from that commit started against it with `ddl-auto=validate`. The disposable
database was deleted afterwards.

Rollout. Supabase's migration history already held five entries from the 7 and 13 October
hosted rollouts under their own timestamps. Placeholder files with those versions let
`supabase db push --linked` match them, so it applied only the new file, preceded by the
application's advisory lock (`pg_advisory_xact_lock(4422026)`), at 10:50 UTC. The history now
also records version `20261023090000`. The E2 file's change to `NOT VALID` was not replayed on
the shared database; it only matters for future full replays, where the new file revalidates
the final list. Before the rollout, the `postgres` role held 12 connections and no query was
running; the count was the same on a second check.

Post-rollout checks confirmed:

- `group_join_requests_status_check` allows `WITHDRAWN`, and `notifications_type_check` allows
  `GROUP_JOIN_REQUEST_WITHDRAWN`, `GROUP_MEMBER_LEFT` and the six session types. Both are validated.
- A second schema dump differs from the backup only in those two constraints.
- All 21 `public` tables have the same row counts as the backup.
- The packaged application from `9bd9264` started against the shared database with
  `ddl-auto=validate`, demo seeding and live weather disabled, then was stopped.

Compatibility: the backend before PR #67 has no `WITHDRAWN` status or the two new
notification types. It works against this schema until the new code writes one of them; after
that, an older backend fails on screens that load those rows. Pull `main` once #67 merges
before running the backend against the shared database again.

### Study room invitation type — 24 October 2026

`20261024010000_study_room_started_notification.sql` only widens `notifications_type_check` to
allow `GROUP_STUDY_ROOM_STARTED`; it changes no rows. It was applied to the shared database at
about 01:25 SGT, directly with `psql` from the `postgres:17` image in one transaction, because a
branch backend already writing the new type had made starting a study-room timer fail.

- No backup or dry run on a restored copy was taken, unlike the earlier rollouts. Before
  applying, a read-only check found every existing notification type already in the new list.
- Supabase's migration history does **not** record version `20261024010000`. The file drops
  and re-adds the constraint, so a later `supabase db push` or `scripts/migrate-database.sh`
  can apply it again without effect.
- Afterwards the constraint allowed the new type and was validated, and the table still held
  its 31 notifications.

The backend also sends these invitations in a separate transaction after the timer start
commits, so a database missing this file logs the failure instead of rejecting the start.

### Quizzes — 25 October 2026

`20261025090000_quizzes.sql` (branch `feat/team-b/group-quiz`) only creates five new tables:
`quizzes`, `quiz_questions`, `quiz_question_options`, `quiz_attempts` and
`quiz_attempt_answers`, with RLS on and browser roles revoked. It changes no existing table or
row. It was applied to the shared database at about 23:02 SGT, directly with `psql` from the
`postgres:17` image in one transaction (`--single-transaction`, `ON_ERROR_STOP`), so the branch
backend could be tested against shared data.

- No backup or dry run on a restored copy was taken, because the file only adds tables. Before
  applying, a read-only check found none of the five tables present and all four referenced
  tables (`courses`, `students`, `study_groups`, `study_sessions`) in place.
- Supabase's migration history does **not** record version `20261025090000`. Every statement
  uses `IF NOT EXISTS` or is a repeatable grant, so a later `scripts/migrate-database.sh` applies
  it again without effect.
- Afterwards all five tables had RLS on, `anon` could not select from `quizzes`, and `users`
  still held its 55 rows.
- The file is now applied: change the quiz schema only with a new migration.

## Privacy and access verification

After the recorded merged E2 migration set, all 20 application tables had RLS enabled;
PUBLIC/anon/authenticated grants are revoked and there are no browser policies. This
baseline boundary, including enabled criteria, passed the 13 October disposable tests.
The room/session boundary was checked on the shared project after the
[16 October E2 rollout](#e2-rollout--16-october-2026). The backend owner is intentionally not FORCE-RLS, so
JDBC still works. Revocation of browser sequence and future default table/sequence/function
privileges prevents an alternate direct API data path. Do not add a permissive policy to make
the dashboard's advisor INFO notice disappear.

```sql
SELECT relname, relrowsecurity, relforcerowsecurity
FROM pg_class WHERE relnamespace='public'::regnamespace AND relkind='r';
SELECT table_name, grantee, privilege_type
FROM information_schema.role_table_grants
WHERE table_schema='public' AND grantee IN ('PUBLIC','anon','authenticated');
```

The second query must return no rows for application tables. In an isolated authorized test
session, SET ROLE anon/authenticated must make student/contact/user/notification reads and
table writes fail with SQLSTATE 42501, while normal backend operations pass. These checks
are in `DatabaseBoundaryTest`. The historical 7 October hosted advisor review found no
security WARNING/ERROR; this does not claim a new advisor check after PR #54 rollout.
The remaining [RLS enabled/no policy INFO](https://supabase.com/docs/guides/database/database-linter?lint=0008_rls_enabled_no_policy)
is intentional denial of browser access. Newly created indexes can also appear as
[unused-index INFO](https://supabase.com/docs/guides/database/database-linter?lint=0005_unused_index)
on a small fresh dataset; unique/foreign-key/query indexes are retained for their documented
invariants and access patterns. Reassess measured use after real workload, not immediately
after creation. Natural goal-set primary keys resolve the previous no-primary-key notices.

## Recovery and rollback

Each migration invocation is transactional: a preflight/DDL error rolls back that invocation.
The historical 7 October hosted work applied files as individual invocations; an interruption
between them requires inspecting history and resuming the unapplied work. The 13 October
compulsory rollout bundled all seven source files into one hosted transaction and one history
entry. Its failure would roll back that complete invocation. The local runner also applies
the complete sequence in one transaction. Check the actual deployment record rather than
assuming every hosted upgrade used the same invocation boundaries.

For a serious populated-schema rollback, stop incompatible writers, restore a tested private
backup to an isolated target first, verify data/sequence state and permissions, then use a
reviewed recovery window. Do not automatically drop tables, reverse timestamp conversion,
remove privacy grants or point an old application at new data. For normal bug fixes prefer a
forward migration. Permanent account deletion is intentionally irreversible through the UI;
provider backup restoration is an operator recovery task, not Reactivate.

References: [PostgreSQL partial indexes](https://www.postgresql.org/docs/17/indexes-partial.html),
[locking](https://www.postgresql.org/docs/17/explicit-locking.html),
[Supabase RLS](https://supabase.com/docs/guides/database/postgres/row-level-security),
[securing the API](https://supabase.com/docs/guides/api/securing-your-api).

## PR #52 E2 upgrade

The ordered set now includes the original room migration and two forward E2 migrations.
They were applied to shared Supabase on 16 October 2026; see the
[E2 rollout record](#e2-rollout--16-october-2026). Do not revise an
already-applied room migration or use Hibernate `update` as a substitute for the
audio adoption and security constraints. Inspect actual target history/schema first;
the historical compulsory rollout above remains a baseline-only record.

The original room table retains its legacy single-audio columns for repeatable upgrades.
The layer migration backfills only rows with missing layer-playing flags: a legacy
music choice becomes the music layer and a legacy ambient choice becomes the ambient
layer. Repeating SQL must preserve current selections and never restart paused audio.
New clients use independent music/ambient fields; old single-preset clients are not
the E2 API contract. Deploy the upgraded backend and frontend together.

Room audio was removed from the application on 24 October. The backend no longer maps any
`study_rooms` audio column; all of them have defaults or allow null, so new rooms still
insert. They stay in place until a dropping migration is applied after every running
backend has the change, because older backends still write them.

`study_sessions` belongs to exactly one group or connection, enforced by a database
check. Its local date-time fields are intentional civil times in the saved IANA zone;
its UTC first-occurrence fields and event timestamps use `timestamptz`. They must not
go through the earlier legacy event-time conversion. Occurrence count is bounded
1–52, each end follows its start, and cancellation timestamp/status agree.
Parent deletion cascades the session; deleting a creator sets its audit reference to
null. Application lifecycle collaborators cancel affected future series and remove
session-linked notifications before permanent parent cleanup.

Before E2 rollout, restore a private populated backup into disposable PostgreSQL and
verify the complete ten-file order, repeated migrations, legacy audio adoption,
all 20 mapped-table RLS/grants, normal JDBC mapping, retained baseline data and session
calendar/lifecycle behavior. Capture only counts/checksums/schema facts in shareable
evidence, never backup contents/password hashes/contacts. The normal runner takes the
exclusive application advisory lock and wraps the complete sequence in one transaction.
Follow the populated-database preflight/rollback procedure above.

After rollout, verify all three E2 tables and sequences deny `PUBLIC`, `anon` and
`authenticated`, constraints/indexes are installed, the application starts with
`ddl-auto=validate`, and the deployed source's room/session workflow checks pass. Weather
uses a bounded in-memory backend cache and adds no database table or browser grant.
Record the actual target/time/migration-history name separately from local results in
[E2_TESTING.md](E2_TESTING.md).
