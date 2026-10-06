# Database operations

The application uses backend JDBC with application JWTs. It does not use a browser Supabase
SDK or Supabase Auth policies. SQL changes touch the 16 mapped public application tables,
their sequences/default privileges and browser grants; provider-managed schemas are untouched.

## Ordered migrations

| Local SQL file | Purpose |
| --- | --- |
| `20261006170044_studybuddy_baseline.sql` | Existing mapped schema; CREATE IF NOT EXISTS for fresh or matching databases |
| `20261006170327_team_c_integrity.sql` | Preflight, normalized identity, JWT version/last login, request context, safe event metadata, description width, UTC conversion, checks/unique/query indexes |
| `20261006170448_backend_only_database_access.sql` | RLS and deny browser table/sequence grants; future default privileges follow the same model |
| `20261006172934_collection_goal_primary_keys.sql` | Natural owner/goal primary keys for the two set collections; preserve valid rows and reject legacy null goals |

These are versioned source files, generated using the existing Supabase CLI. The shared
MCP migration runner records its own deployment timestamps, so do not expect those history
versions to equal local filename prefixes. Match history by migration name. Do not modify
an applied file to change a deployed schema: add another migration.

`scripts/migrate-database.sh` wraps all files in one transaction with ON_ERROR_STOP and the
exclusive application advisory lock. It is intentionally repeatable, including existing-schema
adoption. The shared hosted migrations are already applied as of 7 October 2026.

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
5. Apply files in order and verify mappings, constraints and access. `ddl-auto=validate`
   deliberately refuses an old/mismatched schema. Never re-enable Hibernate update to bypass it.

The shared upgrade preflight found the mapped database empty. An ignored private schema
snapshot (`.env.shared-schema-backup.json`) captured columns, constraints, indexes, grants,
RLS, policies and counts before changes; there were no application rows to export. No shared
table was reset, truncated or deleted for verification. Destructive tests used disposable local
PostgreSQL only. See [testing evidence](TEAM_C_TESTING.md) for the actual run.

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
the seed finished. The current shared run has 10 courses, 50 students, 51 users and passes a
second startup without identity/password/creation changes.

## Privacy and access verification

All 16 application tables have RLS enabled; PUBLIC/anon/authenticated grants are revoked,
and there are no browser policies. The backend owner is intentionally not FORCE-RLS, so
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
are in `DatabaseBoundaryTest`. Hosted advisor review found no security WARNING/ERROR;
the remaining [RLS enabled/no policy INFO](https://supabase.com/docs/guides/database/database-linter?lint=0008_rls_enabled_no_policy)
is intentional denial of browser access. Newly created indexes can also appear as
[unused-index INFO](https://supabase.com/docs/guides/database/database-linter?lint=0005_unused_index)
on a small fresh dataset; unique/foreign-key/query indexes are retained for their documented
invariants and access patterns. Reassess measured use after real workload, not immediately
after creation. Natural goal-set primary keys resolve the previous no-primary-key notices.

## Recovery and rollback

Each migration invocation is transactional: a preflight/DDL error rolls back that invocation.
The hosted runner applies each file separately, so inspect migration history after an interrupted
deployment and resume the unapplied file; do not pretend the whole sequence was one hosted
transaction. The local runner applies the sequence as one transaction.

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
