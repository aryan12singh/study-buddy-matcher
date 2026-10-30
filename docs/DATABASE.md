# Database

The application stores its data in Supabase Postgres. This page covers how the database is
accessed, how the schema is built and changed, how demo data is seeded and reset, and the
operational runbook.

## Access model

- **Backend-only JDBC.** The Spring Boot backend is the only database client. Browsers call
  the Spring API with the application's own JWTs; the frontend has no Supabase browser SDK
  and never talks to PostgREST or Supabase Auth.
- **Row Level Security with no browser policies.**
  `20261006170448_backend_only_database_access.sql` enables RLS on every baseline table,
  revokes all table and sequence privileges from `PUBLIC`, `anon` and `authenticated`, drops
  any existing policies, and revokes the same roles' default privileges on future tables,
  sequences and functions. Every later migration that creates a table enables RLS on it and
  revokes the same grants in the same file. No permissive policy exists, so the browser roles
  can read and write nothing.
- **The backend owner is not `FORCE`d through RLS**, so normal JDBC access is unaffected.
  Supabase's advisor reports an informational "RLS enabled, no policy" notice for each table;
  that is the intended denial, and adding a permissive policy to silence it would open a
  direct data path.
- **Schema comes only from migrations.** `application.yml` sets
  `spring.jpa.hibernate.ddl-auto: validate`: Hibernate never creates or alters a table, and
  startup fails, naming the table or column, when a mapping does not match the database.
  Validation does not check constraints, indexes, primary keys or RLS, so every migration
  must still be applied. Do not override this setting to `update`.

`DatabaseBoundaryTest` checks the boundary: as `anon` or `authenticated`, reads of student,
contact, user and notification data and table writes fail with SQLSTATE `42501`, while
normal backend operations succeed.

## Applying migrations

`scripts/migrate-database.sh` applies every file in
`backend/src/main/resources/db/migrations`. It reads the connection only from libpq
environment variables, so credentials never appear in arguments.

| Variable | Required |
| --- | --- |
| `PGHOST` | Yes |
| `PGDATABASE` | Yes |
| `PGUSER` | Yes: the owner/backend role, never `anon` or `authenticated` |
| `PGPASSWORD` | Yes: set it privately in the shell |
| `PGPORT` | No: libpq defaults to 5432 |

```bash
export PGHOST=<host> PGDATABASE=<database> PGUSER=<owner>
# Set PGPASSWORD privately in this shell; the script refuses to run without it.
scripts/migrate-database.sh
```

How it runs:

- **One transaction.** All files go through a single `psql --single-transaction` session with
  `ON_ERROR_STOP=1`. Any error, including a preflight rejection, rolls back the whole run, so
  a database is never left half-upgraded.
- **Advisory lock.** The run first takes `pg_advisory_xact_lock(4422026)`, the same lock the
  application takes for account-lifecycle changes, so two migration runs, or a run and an
  account change, cannot interleave.
- **File order.** Files are applied in the order the shell glob `*.sql` returns them, which is
  sorted by full filename. The timestamp prefix therefore sets the order.
- **Repeat-safe.** Every file is idempotent (`IF NOT EXISTS`, drop-then-add constraints,
  guarded backfills), so running the whole set again against an up-to-date database changes
  nothing. It is also how an existing database is adopted: the baseline creates only missing
  tables. Repeating it is not a data reset.

Never edit a migration that has been applied to a shared database; add a new one.

## Migrations

| File | What it does |
| --- | --- |
| `20261006170044_studybuddy_baseline.sql` | Creates the 16 baseline tables with `CREATE TABLE IF NOT EXISTS`, safe on a fresh or matching existing database |
| `20261006170327_team_c_integrity.sql` | Preflight that aborts on incompatible data (duplicate normalised emails, duplicate pending requests or active connections, self pairs, invalid years, capacities, slots, over-capacity groups); normalises emails; adds token version, last sign-in, request context and notification event metadata; converts legacy local times to UTC once; adds named checks, unique and query indexes |
| `20261006170448_backend_only_database_access.sql` | Enables RLS, revokes browser-role grants and default privileges, drops existing policies |
| `20261006172934_collection_goal_primary_keys.sql` | Gives `student_study_goals` and `study_group_goals` natural `(owner, goal)` primary keys; rejects null goals instead of deleting rows |
| `20261007220000_match_request_cancellation.sql` | Adds `CANCELLED` to match request status and its notification type, distinct from `DECLINED` |
| `20261009010000_study_rooms.sql` | Creates `study_rooms` (timer state per group) and `room_presences` (expiring per-tab leases) |
| `20261011090000_matching_weights_per_strategy.sql` | Adds `matching_configs.strategy`; existing rows become Balanced; one weight per strategy and criterion |
| `20261013090000_complete_matching_configuration.sql` | Singleton `matching_strategy_settings` row with minimum score, revision and override flag; creates `matching_enabled_criteria`; rejects multiple legacy settings rows |
| `20261014010000_room_audio_layers.sql` | Adds independent music and ambient columns to `study_rooms` and backfills them once. Room audio has since been removed from the application and these columns are no longer mapped; the file stays so the ordered history still applies |
| `20261014020000_study_sessions.sql` | Creates `study_sessions` for one group or one connection, with weekly occurrences (1 to 52), local and UTC times and a calendar UID; adds session notification types |
| `20261016090000_match_explanation_cache.sql` | Creates `match_explanations`, a cache of AI match explanations keyed by a hash of anonymous evidence, holding no student IDs |
| `20261023090000_study_plans.sql` | Creates `study_plans` and its four child tables for AI study roadmaps; stores file names and extracted topics, never uploaded files |
| `20261023090000_withdraw_and_leave.sql` | Adds `WITHDRAWN` to group join request status and the withdrawn and member-left notification types; relabels earlier withdrawals recorded as rejections |
| `20261024010000_study_room_started_notification.sql` | Adds `GROUP_STUDY_ROOM_STARTED` to the notification types |
| `20261025090000_quizzes.sql` | Creates `quizzes`, `quiz_questions`, `quiz_question_options`, `quiz_attempts` and `quiz_attempt_answers` |
| `20261025233000_untimed_personal_quizzes.sql` | Makes `quizzes.closes_at` nullable for personal quizzes, keeps it required for group quizzes, clears it on existing personal quizzes |
| `20261028090000_earned_badge_tiers.sql` | Creates `earned_badges`: the highest tier per student and badge, unique per pair, cascading on student deletion; RLS on and browser grants revoked |
| `20261029090000_room_reactions_and_focus_forest.sql` | Creates `room_reactions` (short-lived, pruned on send) and `focus_visits` (one row per room visit with its server-drawn tree `variant` and meadow `plot`, at most one open per student and group, cascading on student deletion, group set null on group deletion); adds `FORESTER` to the earned badge check; RLS on and browser grants revoked |

**Shared prefix.** `20261023090000_study_plans.sql` and `20261023090000_withdraw_and_leave.sql`
have the same timestamp. Because the glob sorts by full filename, `study_plans` always runs
before `withdraw_and_leave`. The two files touch unrelated tables, so their relative order
does not matter beyond being stable.

**Notification type list.** Several files redefine `notifications_type_check`. Earlier files
add their subset `NOT VALID` so a full repeat run does not reject rows saved with newer
types; the latest file in the set adds the complete current list and validates it against
every row.

## Schema overview

The migrations create 34 tables in `public`.

| Area | Tables |
| --- | --- |
| Accounts and profiles | `users`, `students`, `courses`, `student_courses`, `student_study_goals`, `availability_slots` |
| Buddies | `match_requests`, `connections` |
| Groups | `study_groups`, `study_group_goals`, `group_availability_slots`, `group_memberships`, `group_join_requests` |
| Notifications | `notifications` |
| Achievements | `earned_badges` |
| Matching configuration | `matching_configs`, `matching_strategy_settings`, `matching_enabled_criteria` |
| Collaborative study | `study_rooms`, `room_presences`, `room_reactions`, `study_sessions` |
| Focus forest | `focus_visits` |
| AI features | `match_explanations`; `study_plans`, `study_plan_source_files`, `study_plan_source_topics`, `study_plan_entries`, `study_plan_entry_tasks`; `quizzes`, `quiz_questions`, `quiz_question_options`, `quiz_attempts`, `quiz_attempt_answers` |

Points worth knowing:

- `students` shares its primary key with `users`; an administrator is a `users` row with no
  profile.
- Unique partial indexes enforce one pending request per unordered pair of students, one
  active connection per pair, and one pending application per student and group.
- `matching_strategy_settings` always has a single row (`id = 1`) holding the active strategy,
  minimum score, revision and whether stored values override the startup defaults.
- `study_sessions` belongs to exactly one group or one connection (a database check).
  Deleting the parent cascades to the session; deleting its creator sets the creator to null.
- Event timestamps are `timestamptz` and Hibernate writes in UTC. Session local times are
  civil times in the session's saved IANA zone.

## Demo seed

The seeder in `backend/src/main/java/com/studybuddy/seed` is opt-in. `DemoSeedBootstrap` runs
only when `app.demo-seed.enabled` is `true`.

| Key | Environment variable | Default |
| --- | --- | --- |
| `app.demo-seed.enabled` | `DEMO_SEED_ENABLED` | `false` |
| `app.demo-seed.student-password` | `DEMO_STUDENT_PASSWORD` | empty |
| `app.demo-seed.admin-password` | `DEMO_ADMIN_PASSWORD` | empty |

To seed, set these in the environment or in the git-ignored
`backend/src/main/resources/application-local.yml`, start the backend once, wait for the
seed to finish, then set `enabled` back to `false`. Both passwords must satisfy the normal
password rules or the seed stops. The values are supplied privately by the operator and are
never recorded in tracked files, frontend env files or pull requests.

What it does, in one transaction under the same advisory lock:

- reads `backend/src/main/resources/demo-seed/demo-data.json`;
- adds any missing courses (14), the admin and 50 synthetic students, all under
  `@demo.example.test`, through the normal account and profile services, so normal
  validation applies;
- only when none of the demo students existed beforehand, replays the demo history through
  the real services, with notifications produced as in live use:
  - match requests ending as pending, accepted, accepted then ended, declined (`DECLINED`) or
    withdrawn by the sender (`CANCELLED`); whoever answered a request or application has its
    notice marked read, so only items still open stay unread;
  - five groups, one of them full and one closed, and one deactivation. A group with
    `formedWeeksAgo` (the presenter's IS442 Design Patterns Clinic, four weeks) is back-dated
    by `DemoGroupHistory`, which sets `study_groups.created_at` and its members'
    `group_memberships.joined_at` with SQL, on the rows the seed has just created only. The
    services always record joining as now, and achievements count meetings from the join
    time, so without it the Clinic's earlier meetings would not count towards the streak;
  - the presenter's group and buddy sessions at their time block's next occurrence, or
    `weeksAgo` weeks earlier for a weekly series already under way (the Clinic began three
    weeks ago);
  - quizzes taken by their `takers`, each with its `secondsTaken`: a group quiz linked to the
    group session named in `session` and closed for review, a submitted practice quiz and an
    open one left to take;
  - a personal study plan with some topics ticked off, and from `sessionPlans` a roadmap for the
    Clinic's remaining meetings, with entries for meetings the series no longer has left out;
  - finished study room visits, so the presenter's focus forest shows every tree species.

  The quiz questions and roadmaps are written in advance in the data file and pass the same
  validators as Gemini's replies, so no AI call is made. Relative dates are resolved when the
  seed runs, in each feature's time zone.

Existing courses, profiles and password hashes are left as they are. The seed never resets,
repairs or reactivates existing data. It runs after startup, so startup log messages appear
before it completes; if the application stops mid-seed the transaction rolls back. It is
complete when `select count(*) from study_groups where name = 'IS212 Sprint Retro'` returns 1.

## Demo reset

`scripts/sql/reset-demo-data.sql` removes every `@demo.example.test` account and everything
depending on it, so the seed can rebuild the demo from scratch:

- **Removes:** demo students and the demo admin, their profiles, courses taken, goals and
  availability; groups led by demo students with their memberships, applications, slots and
  goals; requests and connections involving a demo student; study sessions under those
  groups and connections; notifications sent to demo students or pointing at a removed
  student, group, request or session. Quizzes, quiz attempts, study plans, earned badges, room
  reactions and focus visits go with their student or group through `ON DELETE CASCADE`; a
  non-demo student's focus visit in a removed demo group keeps its tree, with the group set to null.
- **Keeps:** courses, matching configuration and every non-demo account. A non-demo account
  loses only its own links to demo accounts.

It is a single `DO` statement under `pg_advisory_xact_lock(4422026)`, so it is atomic however
the client sends it: any error changes nothing. It ends by printing
`demo_accounts_remaining = 0`. Run the whole file, not a selection:

```bash
psql --set=ON_ERROR_STOP=1 --file=scripts/sql/reset-demo-data.sql
```

Procedure: stop every backend pointed at the database and tell the team (anything done with
demo accounts is lost), run the reset, start one backend with seeding enabled and wait for
it to finish, then disable seeding.

**Test-only SQL.** `scripts/sql/legacy-migration-fixture.sql`,
`scripts/sql/assert-migrated-legacy.sql` and `scripts/sql/test-reset-demo-data.sql` are used
by `scripts/test-migrations.sh`, which refuses to run unless `PGDATABASE` is the disposable
`studybuddy_migration_test` database. They insert synthetic fixtures and assert results;
never run them against the shared database. `test-reset-demo-data.sql` puts a row tied to a
demo student, group or session in every dependent table (requests, connections, groups,
sessions and notifications, and the newer study room, presence, reaction, focus visit, earned
badge, quiz, question, option, attempt, answer, study plan, topic, entry and task tables), runs
the reset and fails if any of them is left, or if non-demo data was lost. See
[TESTING.md](TESTING.md).

## Runbook

### Changing a database

1. **Back up.** Take a provider snapshot or a `pg_dump` of the application schema and data,
   including sequences and grants, with a `pg_dump` at least as new as the server. Store it
   privately outside the repository; never commit it or attach it to a pull request. For a
   populated database, test-restore it into an isolated database first.
2. **Apply migrations** with `scripts/migrate-database.sh`. If a preflight check rejects
   existing data, resolve the named conflict explicitly; no migration deletes rows to make a
   constraint pass.
3. **Verify.** Start the backend: `ddl-auto: validate` confirms the mappings. Then check that
   no table is readable by browser roles:

   ```sql
   SELECT relname, relrowsecurity
   FROM pg_class WHERE relnamespace = 'public'::regnamespace AND relkind = 'r';
   SELECT table_name, grantee, privilege_type
   FROM information_schema.role_table_grants
   WHERE table_schema = 'public' AND grantee IN ('PUBLIC', 'anon', 'authenticated');
   ```

   Every application table must show `relrowsecurity = true`, and the second query must
   return no rows.

### Releasing a version with new migrations

1. Stop the old backend, so nothing writes while the schema changes and no older code meets
   values it does not understand.
2. Back up and run `scripts/migrate-database.sh` as above.
3. Start the new backend and verify.

### Cautions

- Never run destructive fixtures or test resets against the shared database. Destructive
  tests use disposable local PostgreSQL only.
- Do not use Hibernate `update` as a substitute for a migration: it cannot install the
  checks, RLS, grants or data backfills.
- Fix mistakes with a forward migration. For a serious rollback, stop writers, restore the
  tested backup to an isolated target first, verify it, then switch over. Do not drop tables,
  remove privacy grants or point an old backend at a newer schema.
- Permanent account deletion cannot be undone in the application; restoring from a backup is
  an operator recovery task, not Reactivate.
