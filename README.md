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
- Create/update profile (name, school, programme, year of study, contact number, courses taken) and study preferences (target course, study mode, weekly availability, group size preference, study goals).
- Get a ranked list of compatible students; filter by course, availability, study mode, study goal, minimum match quality.
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
from the API. `Student` and
`StudyGroupLeader` follow the class relationship in the design doc (a student
becomes a group leader by creating a group, rather than a separate hierarchy).
Matching criteria/weights are externalized (DB-configurable via the admin role),
not hardcoded, so the matching strategy can change without a code deploy.

Frontend follows a feature-folder structure (`features/auth`, `features/matching`,
`features/studygroup`, etc.), with shared API client/hooks/components under `shared/`.

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
│           └── connections/ students/ groups/ notifications/ admin/
├── scripts/                              # migration runner and isolated backend tests
├── docs/                                 # API contract, decisions, coverage, diagrams
└── README.md
```

This grows as each team builds their part — packages like `student/`, `matching/`,
`studygroup/`, etc. get created when the code for them actually exists, not scaffolded
ahead of time as empty folders.

## Prerequisites

- JDK 21
- Node.js 22 LTS + npm (matches CI)
- Git
- For the full backend test suite only: Docker, `psql` and OpenSSL
- A Supabase project (ask a team member for an invite to the shared project — see [Environment Variables](#environment-variables))

## Getting Started

```bash
git clone <repo-url>
cd study-buddy-matcher

# Backend
cd backend
# create src/main/resources/application-local.yml — see Environment Variables below
./mvnw clean install

# Frontend
cd ../frontend
cp .env.example .env
# fill in real values in .env (gitignored, never commit it)
npm install
```

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
— no export step, no `.env`, just `./mvnw spring-boot:run` and it works.

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
| `PROFILE_MAX_COURSES` / `app.profile.max-courses` | `8` | Most courses a student can list as currently taken || `DEMO_SEED_ENABLED` / `app.demo-seed.enabled` | `false` | Opt in to the demo seeder for one startup |
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
order with `scripts/migrate-database.sh` (connection from the `PG*` variables above); it is
safe to re-run. `spring.jpa.hibernate.ddl-auto` is `update`, so Hibernate also adds missing
tables and columns on startup, but it never creates the constraints or access rules, so a
fresh database still needs the migrations. The test profile uses `validate` against a
freshly migrated database.

## Running the App

```bash
# Terminal 1 — backend, http://localhost:8080
cd backend && ./mvnw spring-boot:run

# Terminal 2 — frontend, http://localhost:5173
cd frontend && npm run dev
```

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

Students are matched on course, availability overlap, and study mode, with
study goal and group size as secondary criteria. The matching strategy and
criteria weights are admin-configurable (not hardcoded), supporting at least:
- **Balanced Matching** — weighted combination of all criteria.
- **Availability-First Matching** — prioritizes timetable overlap.
- **Course-First Matching** — prioritizes exact course match, then ranks by remaining preferences.

Match quality is shown to users in plain language (e.g. "Strong match"), not a raw score.

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
and [diagrams](docs/diagrams/TEAM_C.md).

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
`integration/`, from the repository root (needs Docker, `psql` and OpenSSL):

```bash
scripts/test-backend.sh
```

It starts a throwaway PostgreSQL 17 container with generated credentials, tests and
applies the migrations, runs every Maven test, then removes the container. It never
touches the shared Supabase project.

**Frontend:**

```bash
cd frontend
npm ci
npm test
npm run lint
npm run build
```

CI runs all of the above on every pull request into `main`.

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

## Team Workflow

- One shared Supabase project for the whole team; don't spin up individual projects.
- Own feature branch → open a Pull Request (PR) to `main` → get teammate review/approval → merge.
- Read [AGENTS.md](AGENTS.md) before making changes.

| Team | Members | Owns |
| --- | --- | --- |
| A | Natthida, Chong Yee, Joanne | Matching engine, search, matching configuration |
| B | Angel, Averyl, Guang Hao | Platform, entities, authentication, own profile, seed, dashboard and shell |
| C | Aryan, Charlize | Requests and connections, profile privacy, study groups, notifications, admin accounts |

Within Team C, Charlize built the `StudyGroup` and `GroupJoinRequest` rules, group queries,
DTOs and services. Aryan built match requests, connections, `ProfileViewAssembler`,
notifications, admin accounts, and the controllers, screens, migrations and integration
tests that connect them. AI use is recorded in [AI_USAGE.md](docs/AI_USAGE.md).
