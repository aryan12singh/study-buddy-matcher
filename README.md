# Study Buddy Matcher System

> Finding a study partner shouldn't be harder than the subject itself.

**Study Buddy Matcher** is a web application that helps university students find
compatible study partners and form study groups — matched by shared courses,
timetable availability, study mode, and study goals, instead of relying on word
of mouth or scattered group chats.

Students maintain a profile and set of study preferences, get ranked matches
from a configurable matching engine, connect with matched peers, and join or
lead study groups. An admin role manages accounts and tunes the matching
engine's criteria and weights, so the matching strategy can evolve without a
code change.

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [User Roles & Features](#user-roles--features)
- [Architecture](#architecture)
- [Folder Structure](#folder-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Running the App](#running-the-app)
- [Browser and Layout](#browser-and-layout)
- [Seed Data](#seed-data)
- [Matching Engine](#matching-engine)
- [Requests, Groups, Notifications and Admin](#requests-groups-notifications-and-admin)
- [Testing](#testing)
- [Libraries](#libraries)
- [Team Workflow](#team-workflow)

## Overview

Three roles use the system:

- **Students** manage a profile + preferences and get matched with compatible peers.
- **Study Group Leaders** (students who create a group) manage group membership.
- **System Administrators** manage accounts and configure the matching engine.

Private information (contact number) stays hidden on a student's public profile
until a match request is accepted — enforced by the backend, not the client.

## Tech Stack

| Layer | Technology | Notes |
|---|---|---|
| Frontend | React (Vite) | talks only to our backend, not the database directly |
| Backend | Java + Spring Boot | handles all the business logic and data access |
| Build Tool | Maven (`mvnw` wrapper) | no separate Maven install needed |
| Auth | Spring Security + JWT | login/session handling |
| Database | Supabase (Postgres) | shared by the whole team |
| ORM | Spring Data JPA / Hibernate | connects our Java code to the database |

## User Roles & Features

**Student**

- See a home page with their next step, activity counts, waiting buddy requests, weekly availability, their study groups and recent notifications.
- Create/update profile (name, school, programme, year of study, contact number, courses taken) and study preferences (target course, study mode, weekly availability, group size preference, study goals).
- Get a ranked list of compatible students; filter by course, availability, study mode, study goal, preferred group size, minimum match quality.
- View another student's public profile (contact number hidden until a match is accepted).
- Send/accept/decline study-buddy requests; view and end active connections.

**Study Group Leader** *(a Student who creates a group — not a separate account type)*

- Create a study group (name, description, study goals, preferred mode, availability, max size).
- View and accept/reject join requests; remove members.

**System Administrator**

- Create/update/delete user accounts; view account status and usage info.
- Configure the matching engine's criteria, weights, and strategy.

## Architecture

Backend follows a layered, package-by-feature structure:
`Controller → Service → Repository → Entity`, with DTOs (Data Transfer Objects)
at the controller boundary so persistence entities are never returned directly
from the API. `Student` composes `User` through a shared `@MapsId` identity. A group's
leader is an ordinary student referenced by that group; STUDENT and ADMIN are the only
account roles.
Matching criteria/weights are externalized (DB-configurable via the admin role),
not hardcoded, so the matching strategy can change without a code deploy.

Frontend follows a feature-folder structure (`features/auth`, `features/matching`,
`features/groups`, etc.), with shared API client/hooks/components under `shared/`.
The [platform overview](docs/diagrams/PLATFORM.md) connects authentication, profiles,
matching and Team C's request/group boundaries. Detailed diagrams remain in the
[Team A](docs/diagrams/TEAM_A.md) and [Team C](docs/diagrams/TEAM_C.md) documents.

## Folder Structure

```
study-buddy-matcher/
├── backend/
│   ├── mvnw, mvnw.cmd, .mvn/            # Maven wrapper
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/studybuddy/
│       │   ├── StudyBuddyApplication.java
│       │   ├── auth/ security/          # login, registration, JWT and role guards
│       │   ├── common/                  # error responses, input rules, account checks
│       │   ├── user/ student/ course/   # shared entities, repositories, course listing
│       │   ├── matchrequest/ connection/ # buddy requests and connections
│       │   ├── profile/ notification/   # profile privacy and notifications
│       │   ├── studygroup/ admin/       # study groups and admin accounts
│       │   ├── matching/               # search, scorers, strategies and config
│       │   └── seed/                    # opt-in demo data
│       ├── main/resources/
│       │   ├── application.yml          # committed, reads env vars, no secrets
│       │   ├── application-local.yml    # gitignored — your real values, see below
│       │   └── db/migrations/           # ordered SQL migrations
│       └── test/
│           ├── java/com/studybuddy/     # unit tests, context-load test, integration/ HTTP tests
│           └── resources/application-test.yml   # isolated test database profile
├── frontend/
│   ├── package.json, vite.config.ts, tsconfig*.json
│   ├── .env.example                     # committed template, no secrets
│   └── src/
│       ├── App.tsx, routes.tsx           # route tree
│       ├── main.tsx, index.css
│       ├── shared/                       # API client, auth context, guards, UI components
│       └── features/
│           ├── landing/LandingPage.tsx
│           ├── auth/LoginPage.tsx, RegisterPage.tsx
│           └── connections/ students/ groups/ notifications/ admin/ matching/ home/
├── scripts/                              # migration runner and isolated backend tests
├── docs/                                 # API contract, decisions, coverage, diagrams
└── README.md
```

These folders describe the implemented compulsory application. Selected extras are tracked
separately in the [coverage map](docs/REQUIREMENT_COVERAGE.md#selected-extras-and-delivery).

## Prerequisites

- JDK 21 selected by `JAVA_HOME` and `java` (check `java -version`)
- Node.js 22 LTS + npm (matches CI)
- Git
- `psql` for database migrations; OpenSSL to generate signing keys and test credentials
- For the full backend test suite: Bash and a running Docker daemon with access to `postgres:17`
- A Supabase project (ask a team member for an invite to the shared project — see [Environment Variables](#environment-variables))

## Getting Started

```bash
git clone https://github.com/aryan12singh/study-buddy-matcher.git
cd study-buddy-matcher

# Backend development package
cd backend
# create src/main/resources/application-local.yml — see Environment Variables below
./mvnw -DskipTests package

# Frontend
cd ../frontend
cp .env.example .env
# set VITE_API_BASE_URL in .env if the backend is not on the example address
npm ci
```

This development package command skips tests. `application-local.yml` configures the running
application; it does not provision the isolated test database. Run the
[full backend harness](#testing) separately from the repository root.

Before starting the backend, the target database must have the ordered migrations below.
Coordinate the shared-project rollout with its operator using
[DATABASE_OPERATIONS.md](docs/DATABASE_OPERATIONS.md); a successful local package or test
run does not prove that the shared schema has been upgraded.

## Environment Variables

These are **never committed**. Get real values from the shared Supabase project
(Project Settings → Database).

**Backend — `backend/src/main/resources/application-local.yml`** (gitignored, create it yourself):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://<host>:5432/postgres
    username: postgres
    password: <supabase-db-password>
jwt:
  secret: <generate with: openssl rand -base64 48>
```
`application.yml` defaults `spring.profiles.active` to `local`, so this file loads automatically
when running `./mvnw spring-boot:run`. A reachable, migrated database and a valid signing key
are still required. Keep this file private and ignored by Git.

**Frontend — `frontend/.env`** (copy from `.env.example`):
```
VITE_API_BASE_URL=http://localhost:8080/api
```
Vite reads this file automatically.

### Configuration reference

Only `VITE_`-prefixed values reach the frontend, and they end up in the public bundle, so
never put a secret there. Database credentials, the JWT secret and demo passwords belong
in `application-local.yml` or the process environment.

| Key | Default / requirement | Purpose |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` | Loads `application-local.yml` in development |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Required unless set in `application-local.yml` | Backend database connection |
| `JWT_SECRET` / `jwt.secret` | Required, at least 32 characters | Signs login tokens; use a random value |
| `JWT_EXPIRATION_MS` / `jwt.expiration-ms` | `86400000` (24 hours) | Token lifetime in milliseconds |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | Comma-separated frontend origins |
| `app.auth.secret`, `app.auth.token-lifetime`, `app.auth.allowed-origins` | Bound from the three settings above | Typed authentication config (`AuthProperties`) |
| `PROFILE_GROUP_SIZE_MAX` / `app.profile.group-size-max` | `5` (min `3`) | Largest group a "small group" or "either" preference covers |
| `PROFILE_MAX_COURSES` / `app.profile.max-courses` | `8` | Most courses a student can list as currently taken |
| `app.matching.default-strategy` | `BALANCED` | Strategy used until an admin chooses one (`BALANCED`, `AVAILABILITY_FIRST`, `COURSE_FIRST`) |
| `app.matching.default-weights.<strategy>.*` | balanced: course `0.30`, availability `0.30`, study-mode `0.20`, study-goal `0.10`, group-size `0.10`; availability-first: availability `0`, course `0.40`, study-mode `0.30`, study-goal `0.15`, group-size `0.15`; course-first: course `0`, availability `0.40`, study-mode `0.30`, study-goal `0.15`, group-size `0.15` | Every strategy has all five weights, each from 0 to 1. Enabled weights add up to 1; disabled weights must be 0. The priority criterion is editable, including 0 |
| `app.matching.default-enabled-criteria` | All five criteria | Global startup enabled criteria; disabled criteria must have zero weights |
| `app.matching.default-min-score` | `0` | Minimum total compatibility returned, before a student's additional quality filter |
| `app.matching.default-result-limit` / `app.matching.max-result-limit` | `20` / `50` | Default and largest search result count, applied after filtering and sorting |
| `app.matching.quality.strong` / `app.matching.quality.good` | `0.75` / `0.50` | Weighted-total cut-offs for the Strong and Good match labels; anything lower is Fair |
| `DEMO_SEED_ENABLED` / `app.demo-seed.enabled` | `false` | Opt in to the demo seeder for one startup |
| `DEMO_STUDENT_PASSWORD` / `app.demo-seed.student-password` | Required only when seeding | Password for newly seeded demo students |
| `DEMO_ADMIN_PASSWORD` / `app.demo-seed.admin-password` | Required only when seeding | Password for the newly seeded demo admin |
| `VITE_API_BASE_URL` | `/api` when unset | Backend API base URL |
| `VITE_REFRESH_INTERVAL_MS` | `30000` (min `15000`, max `300000`) | Background refresh interval for lists and details |

The test profile reads `STUDYBUDDY_TEST_DATABASE_URL`, `STUDYBUDDY_TEST_DATABASE_USERNAME`,
`STUDYBUDDY_TEST_DATABASE_PASSWORD` and `STUDYBUDDY_TEST_JWT_SECRET`; the migration runner
reads the standard `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER` and `PGPASSWORD`.
`scripts/test-backend.sh` sets all of these itself.

### Database schema

The SQL files in `backend/src/main/resources/db/migrations/` define the full schema:
tables, indexes, integrity constraints and database access rules. Apply them in filename
order with `scripts/migrate-database.sh`; reviewed migrations are safe to re-run.
Set `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER` and `PGPASSWORD` privately to the same target
as the backend datasource, then run from the repository root:

```bash
scripts/migrate-database.sh
```

`spring.jpa.hibernate.ddl-auto` is `update`, so Hibernate may add mapped tables, columns
and some constraints. It does not replace our ordered SQL migrations, legacy-data adoption,
partial/expression indexes, cross-row integrity choices or RLS/grants. Apply the complete
migration set before startup on a fresh or existing database. Merged matching work adds
versioned settings and enabled-criteria storage. The 13 October audit used disposable/local
PostgreSQL; the shared database rollout remained a separate step at that snapshot. See the
[deployment record](docs/DATABASE_OPERATIONS.md#deployment-record-and-pr-54-rollout).
The test profile uses `validate` against a freshly migrated database.

## Running the App

```bash
# Terminal 1 — backend, http://localhost:8080
cd backend && ./mvnw spring-boot:run

# Terminal 2 — frontend, http://localhost:5173
cd frontend && npm run dev
```

### Troubleshooting

| Symptom | Check |
| --- | --- |
| Java release/compiler error | Select JDK 21 in `JAVA_HOME` and verify `java -version` in this terminal |
| `clean install` or context tests cannot connect to a database | Use the development package command above, or `scripts/test-backend.sh` for the isolated full suite; local application YAML is not the test profile |
| Backend connection, schema validation or missing-column error | Check the private datasource values and the ordered migration/rollout record for the actual target |
| Test harness cannot start or connect | Start Docker, ensure `psql` and OpenSSL are on `PATH`, and allow the PostgreSQL 17 image to download |
| Browser cannot call the API | Check `VITE_API_BASE_URL`, backend startup and `CORS_ALLOWED_ORIGINS`; restart Vite after changing `.env` |
| No demo students/courses | The seeder is opt-in and requires private passwords; use the seed procedure below on the approved target |

## Browser and Layout

The application uses decorative window chrome with ordinary routes, a side navigation on
wide screens and wrapping navigation/forms on narrow screens. Shared styles adapt schedules
and admin tables for narrower layouts. Tahoma/Verdana/system fonts keep body text conventional;
pixel art comes from CSS drawings rather than a required pixel font.

Local Chromium student/admin workflows, keyboard checks and a 390 px Home smoke check are
recorded in the [Team A](docs/TEAM_A_TESTING.md) and [compulsory integration](docs/TEAM_C_TESTING.md)
guides. These checks cover the stated scenarios. Latest-Figma comparison, complete browser/device
coverage, accessibility review and physical-projector readability remain team acceptance work.

## Seed Data

Per project requirements, the database must contain at least **10 courses** and
**50 student profiles**. This runs against the one shared Supabase DB — the seed
only needs to be run **once by one team member**, not per developer.

The seeder is off by default. To run it, set `DEMO_SEED_ENABLED=true` plus
`DEMO_STUDENT_PASSWORD` and `DEMO_ADMIN_PASSWORD` (private values, never committed) for
one startup, then turn it off again. It only adds what is missing — 10 courses, 50
synthetic students (`priya@demo.example.test`, `jamie@demo.example.test`,
`alex@demo.example.test`, `student04@demo.example.test` … `student50@demo.example.test`)
and one admin (`admin@demo.example.test`) — so repeat runs never duplicate data or reset
passwords.

## Matching Engine

Team A owns the five scorers, interval maths, search, three strategies and admin matching
settings ([issues #1-#4](https://github.com/aryan12singh/study-buddy-matcher/issues/1)).
The **Find buddies** page and the matching preview on **Home** both call `GET /api/matches`.
Home uses the configured default result limit and displays up to three returned students;
it does not impose a hard-coded three-result API limit.
A search requires a selected course or study goal; optional filters narrow the same results.
It excludes the caller, inactive/deleted accounts and existing active connections. Pending
requests remain visible with their state so a second request cannot be sent.

Each criterion returns compatibility from 0 to 1. The total is the sum of each enabled
criterion's score multiplied by its active strategy weight; disabled criteria contribute 0.
These percentages describe preference compatibility, not a probability that a partnership
will succeed. A missing preference scores a neutral 0.5 for an enabled criterion; explicit
filters still require a candidate to meet the selected preference.

| Criterion | Score |
| --- | --- |
| Course | 1 if the candidate targets the course in focus, 0.5 if they only take it, 0 otherwise. The current search course takes precedence over the caller's saved target; no course in focus gives 0.5 |
| Availability | Unique shared weekly minutes divided by the caller's unique free minutes; 0.5 if either has no availability |
| Study mode | 1 for the same mode or either person's `EITHER`; 0 for `IN_PERSON` versus `ONLINE`; 0.5 for a missing mode |
| Study goals | The selected search goal is the focus if supplied; otherwise use the caller's saved goals. Shared focus goals divided by focus goals; 0.5 if the focus or candidate goal set is empty |
| Group size | 1 when the two preferred numeric ranges overlap, 0 when disjoint, 0.5 when either range is missing |

`TimeSlot` is an immutable same-day interval. Overlap uses half-open boundaries: touching
end/start times share no minutes. `WeeklyAvailability` merges duplicate, overlapping and
adjacent blocks on each day before calculating durations or intersections, so shared time
and the heatmap count each minute once. Weekly schedules use campus-local SGT; they are
recurring day/time preferences, not dated events or timezone-converted instants.

Students choose group size categorically, while profiles store a numeric range:
`ONE_TO_ONE` maps to 2-2 people, `SMALL_GROUP` to 3-`app.profile.group-size-max`, and `EITHER`
to 2-maximum. Scoring checks range overlap; the corresponding search filter checks overlap
with the selected category. An `EITHER` filter imposes no restriction.

Disabling a scoring criterion does not remove a student's explicit discovery filters or
sort choices: a course/goal context is still required, mode/time filters still narrow results,
and Most shared hours can still order by actual overlap. Those choices never alter weights.

The active strategy determines **Best match** order:

- **Balanced:** weighted total descending.
- **Availability first:** availability score descending, then weighted total. If availability
  is disabled, use weighted total alone.
- **Course first:** course score descending, then weighted total. If course is disabled, use
  weighted total alone.

The priority criterion may have a weight too. Its equal score within a priority tie adds
an equal term to all tied candidates, so it cannot change their secondary order. Final ties
use case-insensitive name then student ID. **Most shared hours** and **Same year first** apply
that selected comparison first, followed by the strategy order and the same final ties.
The server sorts the complete eligible result set before applying the result limit; it
returns the pre-limit count. The UI preserves this order across quality badges.

Cards show relevant course/goals, programme/year, a total compatibility percentage, a
plain-language quality label, deterministic explanation, criterion bars and unique shared
hours/heatmap. `Strong`, `Good` and `Fair` use the externalised quality cut-offs. The admin's
minimum total score applies to all searches; the student's minimum-quality filter can only
narrow it. Discovery payloads contain no contact number, email, credentials or persistence
entities. Profile viewing and optional-message requests reuse Team C's authorized APIs.
After a successful request the card becomes pending immediately; only its post-send refresh
or a later server response replaces that acknowledgement.

Administrators choose the active strategy, enable/disable criteria globally, edit all three
weight sets and set a minimum score. At least one criterion must remain enabled. Each
strategy's enabled weights must total 1 within the documented decimal tolerance; disabled
weights are 0. Valid fractional weights keep their precision across save, read, editing and
scoring. Invalid saves preserve the last valid state. **Restore defaults** explicitly clears
stored overrides and returns the current `app.matching.*` defaults.

A persistent configuration version increases on every successful save or restore. Updates
supply the version they loaded; stale edits return a conflict instead of overwriting another
administrator's changes. A search reads one consistent configuration/database snapshot,
including the version in its response, so a concurrent save cannot mix old weights with a
new strategy. Stored overrides survive application restart. A future explanation cache must
include this version together with the student/search inputs.

The code lives in `backend/.../matching/`: `MatchService` and `MatchSearchFilter` coordinate
search, `scoring/` contains the `CriterionScorer` implementations and immutable `MatchScore`,
`strategy/` contains `MatchingStrategy` implementations, `availability/` contains interval
value objects, and `config/` owns admin settings. No scorer queries a repository and no
controller queries one directly.

See the [API contract](docs/API_CONTRACT.md#matching-configuration-team-a),
[matching decisions](docs/DESIGN_DECISIONS.md#matching-decisions-team-a),
[coverage map](docs/REQUIREMENT_COVERAGE.md#matching-algorithm-and-scoring-1),
[Team A diagrams](docs/diagrams/TEAM_A.md) and [test/demo guide](docs/TEAM_A_TESTING.md).
The objective's undefined "preferred learning style" remains a professor clarification
under #41/#7; no extra field or category is invented by this implementation.

## Requests, Groups, Notifications and Admin

Built by Team C on top of the platform foundation:

- **Match requests and connections:** send a request with an optional message and
  course/goal context, accept or decline, and end a connection.
- **Profile privacy:** the contact number is in the profile response only for the student
  themselves or an active connection. Group membership and admin access never reveal it,
  and ending a connection removes access immediately.
- **Study groups:** create, browse and filter, apply, approve or reject, edit, remove
  members and close. The leader counts toward capacity.
- **Notifications:** for request and group events, with read state and an unread badge.
- **Admin accounts:** create, edit, deactivate/reactivate and permanently delete accounts,
  with last sign-in, active connections and open group memberships as usage.

See the [API contract](docs/API_CONTRACT.md), [design decisions](docs/DESIGN_DECISIONS.md),
[requirement coverage](docs/REQUIREMENT_COVERAGE.md), [database operations](docs/DATABASE_OPERATIONS.md)
and [diagrams](docs/diagrams/TEAM_C.md). The [Team C test and handoff guide](docs/TEAM_C_TESTING.md)
maps compulsory workflows, regression checks and remaining acceptance steps.

## Testing

- Backend: JUnit + Mockito.
- Frontend: Vitest + React Testing Library.

**Backend unit tests** (no database needed):

```bash
cd backend
./mvnw test -Dtest='!com.studybuddy.integration.**,!StudyBuddyApplicationTests' \
  -Dsurefire.failIfNoSpecifiedTests=false
```

**Full backend suite**, including the context-load test and the HTTP/database tests in
`integration/`, from the repository root (needs JDK 21, Bash, running Docker, `psql` and OpenSSL):

```bash
scripts/test-backend.sh
```

It starts a throwaway PostgreSQL 17 container with generated credentials, tests and
applies fresh/repeated/legacy migrations in private databases, runs every Maven test, then
removes the container. It sets the `STUDYBUDDY_TEST_*` connection/signing values itself.
Integration fixtures clear test data, so use this harness rather than pointing the test
profile at the shared Supabase project.

**Frontend:**

```bash
cd frontend
npm ci
npm test
npm run lint
npm run build
```

CI provisions PostgreSQL 17, applies and repeats migrations, runs the complete backend suite
and the frontend test/lint/build checks on every pull request into `main`, including shared
workflow/SQL/setup changes. The database-free unit-test command is a local shortcut.
Exact dated results and runtime limits belong in [TEAM_A_TESTING.md](docs/TEAM_A_TESTING.md)
and [TEAM_C_TESTING.md](docs/TEAM_C_TESTING.md).

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

The shared retro shell and current landing screen use original CSS pixel drawings in
`PixelIcon.tsx`, CSS shapes/patterns and Unicode glyphs. Tahoma/Verdana/system fonts are
requested from the user's system; no font or external icon-set files are bundled.
`index.css` still imports Google Fonts Inter and Fraunces under SIL OFL 1.1
([Inter licence](https://raw.githubusercontent.com/google/fonts/main/ofl/inter/OFL.txt),
[Fraunces licence](https://raw.githubusercontent.com/google/fonts/main/ofl/fraunces/OFL.txt));
network failure falls back to system fonts.

| Asset / service | Origin and distribution record |
| --- | --- |
| `frontend/public/favicon.svg` | Original pixel-book SVG created in the 13 October completion pass; the earlier unverified image was replaced |
| Shared/landing pixel icons, avatar initials and CSS patterns | Original project drawings in `PixelIcon.tsx`, `Avatar.tsx` and `desktop.css`; no external icon set |
| `frontend/public/icons.svg` | Unused sprite removed in the same cleanup; not shipped |
| Google Fonts stylesheet | Existing Inter/Fraunces remote import; SIL OFL 1.1 sources linked above, with system fallback |
| Supabase / PostgreSQL | Hosted database reached only by backend JDBC; local/CI tests use the official PostgreSQL 17 container |

The current landing page has no separate inline SVG asset. No music/AI-provider service is
shipped in the compulsory baseline; selected extras must add their actual service/asset
records when merged. Development assistance is disclosed in `docs/AI_USAGE.md`.

## Team Workflow

- One shared Supabase project for the whole team; don't spin up individual projects.
- Own feature branch → open a Pull Request (PR) to `main` → get teammate review/approval → merge.
- Read [AGENTS.md](AGENTS.md) before making changes.

The team uses 24 October as the internal feature freeze and 25 October as the extras/integration
delivery target. The 15 November 2026 23:59 SGT handoff target must be checked against the actual
LMS deadline; the formal brief states Week 13. See the
[schedule record](docs/DESIGN_DECISIONS.md#schedule-and-submission-record).

| Team | Members | Owns |
| --- | --- | --- |
| A | Natthida, Chong Yee, Joanne | Matching engine, search, matching configuration |
| B | Angel, Averyl, Guang Hao | Platform, entities, authentication, own profile, seed, dashboard and shell |
| C | Aryan, Charlize | Requests and connections, profile privacy, study groups, notifications, admin accounts |

Within Team C, Charlize built the `StudyGroup` and `GroupJoinRequest` rules, group queries,
DTOs and services. Aryan built match requests, connections, `ProfileViewAssembler`,
notifications, admin accounts, and the controllers, screens, migrations and integration
tests that connect them. AI use is recorded in [AI_USAGE.md](docs/AI_USAGE.md).
