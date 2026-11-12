# Scripts

Database and test helpers. The test scripts only run against disposable databases they create
or are given, and no script holds a real credential: connection details come from the
environment, and the local-only scripts generate or use throwaway values.

| Script | What it does | Needs |
| --- | --- | --- |
| [`test-backend.sh`](test-backend.sh) | Starts a private PostgreSQL 17 container with a random password, runs the migration tests, applies the migrations, runs the whole backend suite and removes the container. Extra arguments go to Maven, for example `'-Dtest=MatchServiceTest'` | Bash, Docker, `psql`, OpenSSL |
| [`migrate-database.sh`](migrate-database.sh) | Applies every file in `backend/src/main/resources/db/migrations/` in order, in one transaction and under an advisory lock, so a failed upgrade changes nothing. Safe to repeat | `psql` and the `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` variables |
| [`test-migrations.sh`](test-migrations.sh) | Proves the migrations on an empty `studybuddy_migration_test` database: a legacy upgrade keeps its data, a failed preflight changes nothing, a repeated run is harmless and the demo reset removes only demo accounts. Called by `test-backend.sh` and by CI | The same `PG*` variables, pointing at that empty database |
| [`run-local-backend.ps1`](run-local-backend.ps1) | Windows PowerShell: runs the backend against a private PostgreSQL in Docker, for trying migrations that are not yet on the shared database | PowerShell, Docker |

## `sql/`

| File | Used by | Purpose |
| --- | --- | --- |
| [`reset-demo-data.sql`](sql/reset-demo-data.sql) | A person, before a demo | Removes every `@demo.example.test` account and what depends on it, so the demo seed can rebuild the story. Atomic; every other account is kept |
| [`legacy-migration-fixture.sql`](sql/legacy-migration-fixture.sql) | `test-migrations.sh` | Synthetic rows in the baseline schema, for the legacy-upgrade test |
| [`assert-migrated-legacy.sql`](sql/assert-migrated-legacy.sql) | `test-migrations.sh` | Checks that the upgraded fixture kept its data and gained the new columns and constraints |
| [`test-reset-demo-data.sql`](sql/test-reset-demo-data.sql) | `test-migrations.sh` | Checks that the demo reset removes demo accounts and keeps the rest |

How and when to run them: [testing guide](../docs/TESTING.md) and
[database guide](../docs/DATABASE.md). The diagram scripts live with the diagrams in
[`docs/diagrams/`](../docs/diagrams/README.md).
