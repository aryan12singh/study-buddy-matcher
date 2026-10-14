# Database operations

The application uses backend JDBC with application JWTs. It does not use a browser Supabase
SDK or Supabase Auth policies. The current migration set defines 17 mapped public application
tables, including `matching_enabled_criteria`, plus their sequences/default privileges and
browser grants; provider-managed schemas are untouched. This describes the schema after
applying the complete ordered migration set, not the current hosted deployment state.

## Ordered migrations

| Local SQL file | Purpose |
| --- | --- |
| `20261006170044_studybuddy_baseline.sql` | Existing mapped schema; CREATE IF NOT EXISTS for fresh or matching databases |
| `20261006170327_team_c_integrity.sql` | Preflight, normalized identity, JWT version/last login, request context, safe event metadata, description width, UTC conversion, checks/unique/query indexes |
| `20261006170448_backend_only_database_access.sql` | RLS and deny browser table/sequence grants; future default privileges follow the same model |
| `20261006172934_collection_goal_primary_keys.sql` | Natural owner/goal primary keys for the two set collections; preserve valid rows and reject legacy null goals |
| `20261007220000_match_request_cancellation.sql` | Distinguishes lifecycle cancellation from decline in request and notification constraints |
| `20261011090000_matching_weights_per_strategy.sql` | Matching weights belong to a strategy: adds `matching_configs.strategy` (existing rows become Balanced) and replaces one weight per criterion with one per strategy and criterion |
| `20261013090000_complete_matching_configuration.sql` | Singleton matching settings, minimum score, persistent revision/override state and `matching_enabled_criteria`; preserves valid legacy overrides and denies browser access to the new table |

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

The exact seven source migrations above were then applied transactionally through the hosted
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
5. Apply files in order and verify mappings, constraints and access. The app runs with
   `ddl-auto: update`, but schema update does not replace the project's migration preflight,
   data adoption, partial indexes or browser access rules. Migrations remain required; tests
   use `validate`.

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

The transactional seed takes the exclusive write guard and adds missing course codes and
identities. It supplies 10 courses, 50 synthetic varied profiles and one admin. Existing
course names, profiles and password hashes are preserved. It neither resets nor repairs
existing user data, and does not reseed relations or reactivate an existing identity. If an
operator deletes a seeded identity, a later explicitly enabled seed can recreate it as a new
identity; leave seeding disabled during normal account-lifecycle demonstrations.

Initial handles: priya, jamie, alex, student04 through student50 and admin, all under
`@demo.example.test`. The operator privately supplies passwords. On this machine,
`.env.demo.local` holds the shared demo passwords; source it only for intentional seed use.
Never paste it into a frontend env file, commit it or attach it to a PR.

Verify counts and existing-data preservation through authorized SQL. Compare a checksum
of identity/password/creation fields before/after repeat without printing hashes. App startup
messages precede ApplicationRunner completion; wait for a seed count check before claiming
the seed finished. The historical 7 October shared run recorded 10 courses, 50 students,
51 users and a second startup without identity/password/creation changes. These counts
are historical evidence, not a fresh hosted count for the current branch.

## Privacy and access verification

After the complete migration set, all 17 application tables have RLS enabled;
PUBLIC/anon/authenticated grants are revoked and there are no browser policies. This
boundary, including the new enabled-criteria table, passed the 13 October disposable tests;
it must also be verified after the hosted rollout. The backend owner is intentionally not FORCE-RLS, so
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

## PR #52 room migration

The branch also includes `20261009010000_study_rooms.sql`, adding backend-only
`study_rooms` and `room_presences` with RLS and browser-role grant revocation.
It has not been applied to shared Supabase. Run the ordered migration set before
starting this branch; the baseline matching migrations remain required.
