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
- [Testing](#testing)
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
| Migrations | Flyway | keeps track of database changes over time |

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
│   ├── src/main/java/com/studybuddy/
│   │   ├── config/          # security, CORS config
│   │   ├── security/        # JWT filter/util
│   │   ├── user/            # base user entity/repo
│   │   ├── student/         # profile + preferences
│   │   ├── matching/         # matching engine + scoring
│   │   ├── connection/        # requests, connections, follow/unfollow
│   │   ├── studygroup/        # groups + membership
│   │   ├── admin/             # account mgmt + matching config
│   │   └── course/
│   ├── src/main/resources/
│   │   ├── application.yml            # committed, placeholders only
│   │   └── db/migration/               # Flyway scripts
│   └── src/test/java/...
├── frontend/
│   ├── src/
│   │   ├── features/        # auth, profile, matching, connections,
│   │   │                     # notifications, studygroup, studyroom
│   │   └── shared/          # api client, components, hooks, types
│   └── .env.example
└── README.md
```

## Prerequisites

- JDK 21
- Node.js 20 LTS + npm (or pnpm — confirm with team which one; keep the matching lockfile committed)
- Git
- A Supabase project (ask a team member for an invite to the shared project — see [Environment Variables](#environment-variables))

## Getting Started

```bash
git clone <repo-url>
cd study-buddy-matcher

# Backend
cd backend
cp src/main/resources/application.yml src/main/resources/application-local.yml
# fill in real Supabase values in application-local.yml (gitignored, never commit it)
./mvnw clean install

# Frontend
cd ../frontend
cp .env.example .env.local
# fill in real values in .env.local (gitignored, never commit it)
npm install
```

## Environment Variables

These are **never committed** — only placeholder examples are. Get real values
from the shared Supabase project (Project Settings → Database / API).

**`backend/src/main/resources/application-local.yml`**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://<host>:5432/postgres
    username: postgres
    password: <supabase-db-password>
jwt:
  secret: <shared-or-generated-secret>
supabase:
  url: https://<project>.supabase.co
  service-role-key: <only-if-backend-calls-supabase-api-directly>
```

**`frontend/.env.local`**
```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_SUPABASE_URL=https://<project>.supabase.co
VITE_SUPABASE_ANON_KEY=<anon-key>
```

## Running the App

```bash
# Terminal 1 — backend, http://localhost:8080
cd backend && ./mvnw spring-boot:run

# Terminal 2 — frontend, http://localhost:5173
cd frontend && npm run dev
```

## Seed Data

Per project requirements, the database must contain at least **10 courses** and
**50 student profiles**. Migrations run automatically on backend startup against
the shared Supabase DB — seed scripts only need to be run **once by one team
member**, not per developer, to avoid duplicate data.

## Matching Engine

Students are matched on course, availability overlap, and study mode, with
study goal and group size as secondary criteria. The matching strategy and
criteria weights are admin-configurable (not hardcoded), supporting at least:
- **Balanced Matching** — weighted combination of all criteria.
- **Availability-First Matching** — prioritizes timetable overlap.
- **Course-First Matching** — prioritizes exact course match, then ranks by remaining preferences.

Match quality is shown to users in plain language (e.g. "Strong match"), not a raw score.

## Testing

- Backend: JUnit + Mockito (`./mvnw test`).
- Frontend: Vitest + React Testing Library (`npm test`).

## Team Workflow

- Migrations are added via PR only, never edited after merge (shared DB — edits after merge cause drift).
- One shared Supabase project for the whole team; don't spin up individual projects.
- Own feature branch → open a Pull Request (PR) to `main` → get teammate review/approval → merge.
