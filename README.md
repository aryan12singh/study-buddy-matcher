# Study Buddy Matcher

IS442 Object Oriented Programming, SMU. Students find study partners by course,
availability and preferences, connect by accepting buddy requests, and join or lead
course-specific study groups. Administrators manage accounts and matching settings.

## Current branch status

Team C features are implemented end to end: match requests and connections, profile
privacy, study groups, notifications and admin accounts. See
[requirement coverage](docs/REQUIREMENT_COVERAGE.md).

The branch also includes a shared foundation: login/registration, JWT and role guards,
course listing, demo seed, API client, navigation and UI primitives. Matching (Team A) and
own-profile, availability editor and dashboard (Team B) are integrated separately. Bonus
features go on later branches.

## Architecture and ownership

Java 21, Spring Boot, Spring Data JPA, Spring Security/JWT, Supabase PostgreSQL,
React, Vite, Maven and npm are the fixed stack. Controllers validate input and call
services; services use domain objects/repositories and return assembled DTOs. A
`Student` composes a `User` with a shared primary key. A group leader is its creating
student, rather than a third account role or a separate subclass.

Contact numbers are present only in self or active-buddy profile responses. Membership,
leadership and admin access do not grant contact reads. Ending a connection revokes
subsequent reads. The frontend never connects to Supabase directly; browser roles have
neither application-table grants nor RLS policies. See [API contract](docs/API_CONTRACT.md),
[design decisions](docs/DESIGN_DECISIONS.md) and [Team C diagrams](docs/diagrams/TEAM_C.md).

| Team | Members | Product scope |
| --- | --- | --- |
| A | Natthida, Chong Yee, Joanne | Matching/scorers/strategies, search, matching configuration |
| B | Angel, Averyl, Guang Hao | Platform, entities/repositories, authentication, own profile/preferences, seed, dashboard/shared shell |
| C | Aryan, Charlize | Buddy lifecycle/privacy, groups/membership, notifications, admin accounts and their screens |

Within Team C:

- **Charlize**: `StudyGroup` and `GroupJoinRequest` domain rules (close, leader, capacity,
  accept/reject), group lookup queries and browse filter, group DTOs/assemblers,
  `StudyGroupService` and `GroupJoinRequestService`, and the group endpoint proposal.
- **Aryan**: `MatchRequest` state machine and service, `Connection` and `ConnectionService`,
  `ProfileViewAssembler`, notifications, admin user management and deactivation, and the
  end-to-end integration: HTTP controllers, React screens, the shared auth/client/UI
  foundation, database migrations and the integration tests.

AI tool usage is recorded in [AI_USAGE.md](docs/AI_USAGE.md).

Team C added a small shared foundation (login/registration, JWT, API client, app shell and
UI primitives) so its screens could run before Team B's integration. Reuse it rather than
building a second auth flow, client or shell; `frontend/src/routes.tsx` is the single route
tree for new screens.

```text
backend/src/main/java/com/studybuddy/
  auth/ security/ common/       identity, validation, permissions and write locking
  user/ student/ course/        shared mapped data and course/activity reads
  matchrequest/ connection/     buddy state machine and symmetric connections
  profile/ notification/       privacy assemblers and recipient-scoped events
  studygroup/ admin/ seed/      group/account lifecycle and opt-in demo data
backend/src/main/resources/
  application.yml              public configuration; schema validation
  application-local.yml        ignored personal configuration
  db/migrations/               ordered, repeatable SQL upgrades
backend/src/test/               units, context, real HTTP/PostgreSQL races and privacy
frontend/src/
  features/auth/ landing/       entry points
  features/connections/ students/ notifications/ groups/ admin/
  shared/api/ auth/ components/ shared client, guards, hooks and UI
scripts/                       migration and isolated backend test runners
docs/                          API contract, decisions, coverage, operations and diagrams
```

## Setup and run

Prerequisites: Java 21, Node 22 LTS (22.22.2 or newer), npm and Git. Isolated backend
tests also use Docker, `psql` and OpenSSL. Production data uses the team's shared
Supabase project; disposable local PostgreSQL is only for tests.

1. Install frontend packages using the existing lockfile: `cd frontend && npm ci`.
2. Copy `frontend/.env.example` to `frontend/.env`.
3. Create the ignored `backend/src/main/resources/application-local.yml`:

   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://<database-host>:5432/postgres
       username: postgres
       password: <private-database-password>
   jwt:
     secret: <generate-a-private-value-with-openssl-rand-base64-48>
   ```

4. An operator applies [database migrations](docs/DATABASE_OPERATIONS.md) before startup.
   The Team C shared-project upgrade is already applied; a fresh database needs all four
   SQL files in filename order. Hibernate validates mappings and never installs access
   policies or constraints automatically.
5. Run the two terminals:

   ```bash
   # Repository/backend; application-local.yml loads under the default local profile
   ./mvnw spring-boot:run

   # Repository/frontend
   npm run dev
   ```

Normal URLs: `http://localhost:8080/api` and `http://localhost:5173`. Frontend requests
read `VITE_API_BASE_URL`; the template includes the local API prefix. A deployed JAR
uses process environment variables and an explicit non-local profile. The local credential
YAML remains available for development but is excluded from the distributable JAR.

## Configuration

Only public build settings may use a `VITE_` prefix. Database credentials and JWT/demo
passwords belong in ignored backend configuration or process environment. Never commit
them, put them in screenshots/logs, or pass them to frontend code.

| Key | Default / requirement | Purpose |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` | Loads ignored personal backend config in development |
| `SPRING_DATASOURCE_URL` | Required unless local YAML sets it | Backend JDBC connection |
| `SPRING_DATASOURCE_USERNAME` | Required unless local YAML sets it | Backend database identity |
| `SPRING_DATASOURCE_PASSWORD` | Required unless local YAML sets it | Private JDBC password |
| `JWT_SECRET` / `jwt.secret` | Required, at least 32 UTF-8 bytes | HMAC signing secret; use a random value |
| `JWT_EXPIRATION_MS` / `jwt.expiration-ms` | `86400000` | JWT lifetime in milliseconds, must be positive |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | Comma-separated frontend origins |
| `app.auth.secret` | Alias of `jwt.secret` | Typed authentication setting |
| `app.auth.token-lifetime` | Alias of JWT milliseconds, as a duration | Typed token lifetime |
| `app.auth.allowed-origins` | Alias of CORS origins | Typed origin list |
| `DEMO_SEED_ENABLED` / `app.demo-seed.enabled` | `false` | Explicit opt-in seed bootstrap |
| `DEMO_STUDENT_PASSWORD` / `app.demo-seed.student-password` | Required only when seeding | Private runtime password for new demo students |
| `DEMO_ADMIN_PASSWORD` / `app.demo-seed.admin-password` | Required only when seeding | Private runtime password for the new demo admin |
| `VITE_API_BASE_URL` | Same-origin `/api` when absent | Backend origin or API prefix; template sets localhost API |
| `VITE_REFRESH_INTERVAL_MS` | `30000`; minimum `15000`, maximum `300000` | Bounded list/detail refresh; forms retain edits |

`PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` configure the migration runner.
`STUDYBUDDY_TEST_DATABASE_URL`, `STUDYBUDDY_TEST_DATABASE_USERNAME`,
`STUDYBUDDY_TEST_DATABASE_PASSWORD` and `STUDYBUDDY_TEST_JWT_SECRET` configure the test
profile. The isolated runner supplies them automatically and removes its container.

## Demo seed

The seed is disabled by default. Supply both passwords privately and enable it for one
startup. It adds missing entries only: 10 courses, 50 varied synthetic students and one
admin. Repeat runs preserve existing profiles, passwords and data; there is no shared
reset/truncate endpoint or default password.

Demo identifiers: `priya@demo.example.test`, `jamie@demo.example.test`,
`alex@demo.example.test`, `student04@demo.example.test` through
`student50@demo.example.test`, and `admin@demo.example.test`. Initial passwords come
from the operator's private seed config. Keep them in an ignored file such as
`.env.demo.local` (not a frontend env file). Disable seeding
after use. [Operations](docs/DATABASE_OPERATIONS.md) explains safe repeats and recovery.

## Testing

**Backend unit tests** (no database needed; skips the integration and context-load tests):

```bash
cd backend
./mvnw test -Dtest='!com.studybuddy.integration.**,!StudyBuddyApplicationTests' \
  -Dsurefire.failIfNoSpecifiedTests=false
```

**Full backend suite with Docker.** From the repository root, with Java 21, Docker, `psql`
and OpenSSL installed:

```bash
scripts/test-backend.sh
```

The script starts a throwaway PostgreSQL 17 container on a random local port, generates
private test credentials, runs the migration tests, migrates a `studybuddy_test` database and
runs every Maven test, including the HTTP/database integration tests. It removes the
container and credentials when it exits and never connects to the shared Supabase project.
Test fixtures refuse to clear any database not named `studybuddy_test`.

**Frontend** (Node 22 LTS, 22.22.2 or newer):

```bash
cd frontend
npm ci
npm test
npm run lint
npm run build
```

CI runs the backend against PostgreSQL 17 and the frontend on Node 22 for every PR into `main`.

## Libraries

Versions match the resolved Maven dependencies and the npm lockfile.

| Library/tool | Resolved version | Purpose | Licence / primary source |
| --- | --- | --- | --- |
| Spring Boot JPA, Security, Validation, WebMVC and test starters | 4.1.1 | Fixed stack, wiring and tests | Apache-2.0, [Spring Boot](https://github.com/spring-projects/spring-boot) |
| Spring Framework / Data JPA / Security | 7.0.9 / 4.1.1 / 7.1.1 | Transactions, repositories, web/security | Apache-2.0, [Spring](https://github.com/spring-projects) |
| Hibernate ORM | 7.4.5.Final | JPA persistence | Apache-2.0, [Hibernate](https://github.com/hibernate/hibernate-orm) |
| PostgreSQL JDBC | 42.7.13 | Server-side JDBC | BSD-2-Clause, [pgJDBC](https://github.com/pgjdbc/pgjdbc) |
| JJWT API/impl/Jackson | 0.12.6 | JWT signing/verification | Apache-2.0, [JJWT](https://github.com/jwtk/jjwt) |
| Jackson 3 / Jackson 2 | 3.1.5 / 2.21.5 (annotations 2.21) | Spring JSON / JJWT JSON | Apache-2.0, [Jackson](https://github.com/FasterXML) |
| JUnit Jupiter / Mockito | 6.0.3 / 5.23.0 | Backend tests/mocks | EPL-2.0 / MIT, [JUnit](https://github.com/junit-team/junit-framework), [Mockito](https://github.com/mockito/mockito) |
| Maven / wrapper | 3.9.16 / 3.3.4 | Existing build bootstrap | Apache-2.0, [Maven](https://maven.apache.org/) |
| React / React DOM | 19.3.0 | Components/rendering | MIT, [React](https://github.com/facebook/react) |
| React Router DOM | 7.18.4 | Routes and guards | MIT, [React Router](https://github.com/remix-run/react-router) |
| Axios | 1.20.0 | Shared HTTP client | MIT, [Axios](https://github.com/axios/axios) |
| Vite / React plugin | 8.3.0 / 6.1.1 | Existing dev/build pipeline | MIT, [Vite](https://github.com/vitejs/vite), [plugin](https://github.com/vitejs/vite-plugin-react) |
| TypeScript | 6.0.3 | API/component typing | Apache-2.0, [TypeScript](https://github.com/microsoft/TypeScript) |
| Tailwind CSS / Vite plugin | 4.3.3 | Existing entry styling | MIT, [Tailwind](https://github.com/tailwindlabs/tailwindcss) |
| Oxlint | 1.83.0 | Lint | MIT, [Oxc](https://github.com/oxc-project/oxc) |
| Vitest / jsdom | 5.0.1 / 30.1.0 | Frontend tests/DOM | MIT, [Vitest](https://github.com/vitest-dev/vitest), [jsdom](https://github.com/jsdom/jsdom) |
| Testing Library React / jest-dom | 16.3.3 / 7.0.1 | Rendered interactions/assertions | MIT, [React Testing Library](https://github.com/testing-library/react-testing-library), [jest-dom](https://github.com/testing-library/jest-dom) |
| @types/node / react / react-dom | 24.13.6 / 19.3.0 / 19.3.0 | Development declarations | MIT, [DefinitelyTyped](https://github.com/DefinitelyTyped/DefinitelyTyped) |
| source-map-js (transitive) | 1.2.2 | Existing source-map support; patched lock entry | BSD-3-Clause, [source-map-js](https://github.com/7rulnik/source-map-js), [advisory](https://github.com/advisories/GHSA-68fv-2mgg-jv7q) |

Supabase is the hosted PostgreSQL service, not a browser SDK dependency. Testing uses
official PostgreSQL 17; no embedded database is used.

Team C screens use CSS pixel patterns, Unicode glyphs and Tahoma/Verdana/system fonts;
no icon or font files are bundled. Landing pages load Google Fonts Inter
and Fraunces under SIL OFL 1.1 ([Inter licence](https://raw.githubusercontent.com/google/fonts/main/ofl/inter/OFL.txt),
[Fraunces licence](https://raw.githubusercontent.com/google/fonts/main/ofl/fraunces/OFL.txt)).
The source of `public/favicon.svg`, `public/icons.svg` and the landing page SVGs is not yet
recorded.

## Project delivery

Read [AGENTS.md](AGENTS.md) before changes. Team schedule: 11 October core,
25 October extras/integration, 8 November rehearsal/supporting materials, and
15 November 2026 at 11:59 pm SGT submission; presentation in the Week 13 class slot.

Use feature branches, small commits and a PR into `main`, with review from another team
and green CI before squash merge.
