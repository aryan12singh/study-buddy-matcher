# AGENTS.md

Rules for the eight people and the coding assistants working in this repository. Assistants
load this file automatically, which is how eight people working with AI assistance avoid
producing eight different architectures. The linked documents hold the detail.

## Project overview

Study Buddy Matcher is coursework for IS442 Object Oriented Programming at SMU. Students
register, list courses and weekly availability, get scored matches, send match requests and
form study groups. Administrators manage accounts and tune the matching weights. Two required
extras sit on top: E1 AI study assistance and E2 collaborative study. See
[README.md](README.md) and the [requirement coverage](docs/REQUIREMENT_COVERAGE.md).

The mark rewards object-oriented design, not cleverness: modular layered code, single
responsibility, externalised configuration, conventional Java and meaningful names. Optimise
for a marker reading the code, not for line count. **Feature freeze is in effect:** only
fixes, tests, documentation and demo preparation.

## Stack

Fixed, do not substitute: Java 21, Spring Boot, Spring Data JPA, Spring Security with JWT,
Supabase Postgres, React with Vite, Maven, npm. Do not introduce a library, tool or service
that is not in the [libraries table](README.md#libraries). If you believe one is needed, say
so and stop. If a dependency is agreed, add its row to that table in the same pull request.

## Commands

Run from the repository root unless a directory is given.

| Task | Command |
| --- | --- |
| Run the backend | `cd backend && ./mvnw spring-boot:run` (needs `application-local.yml`, see [Configuration and secrets](#configuration-and-secrets)) |
| Run the frontend | `cd frontend && npm ci && npm run dev` (copy `.env.example` to `.env` first) |
| Full backend suite | `scripts/test-backend.sh` (needs Docker, `psql` and OpenSSL; starts a disposable PostgreSQL 17) |
| Backend unit tests only | `cd backend && ./mvnw test -Dtest='!com.studybuddy.integration.**,!StudyBuddyApplicationTests' -Dsurefire.failIfNoSpecifiedTests=false` |
| Frontend checks | `cd frontend && npm test && npm run lint && npm run build` |
| Apply migrations | `scripts/migrate-database.sh` (reads the `PG*` variables; see [database](docs/DATABASE.md)) |
| Check the diagrams | `python3 docs/diagrams/render_diagrams.py --check` |

First-time setup is in the [README](README.md#getting-started). CI (`.github/workflows/ci.yml`)
runs the backend suite and the frontend checks on every pull request into and push to `main`.

## Layout

```text
backend/    Spring Boot application (Maven), one package per feature under com.studybuddy
frontend/   React application (Vite): src/features/<feature>/, src/shared/
scripts/    Migration runner and the disposable-database test harness
docs/       API contract, decisions, coverage, testing, diagrams (index: docs/README.md)
```

## Architecture: the two rules

Checked on every pull request. Not negotiable.

1. **Controllers never touch repositories.** A controller validates its input, calls one
   service method and returns a DTO. No business logic, no query building, no repository
   injection.
2. **Services never return entities to the API.** Entities stay behind the service boundary.
   Services return DTOs built by an assembler.

```text
Controller -> DTO / Assembler -> Service -> Domain model -> Repository -> DB
```

Rule 2 is also the privacy mechanism. A contact number must be invisible until a match request
is accepted, and `ProfileViewAssembler` enforces that by choosing between `PublicProfileDto`
and `ConnectedProfileDto`. Return an entity from a service and the contact number goes out
with it. Never hide a private field in React and call it done: the value is still in the
payload.

## Java conventions

- Standard naming: `PascalCase` types, `camelCase` methods and fields, `UPPER_SNAKE_CASE`
  constants, lower-case packages.
- Names say what the thing is: `AvailabilityOverlapScorer`, not `Scorer2` or `Helper`. No
  `Util`, `Manager` or `Processor` classes that do six unrelated things.
- One class, one responsibility. If a class needs "and" to describe it, split it.
- Prefer an interface with implementations over a `switch` on a type, as `CriterionScorer` and
  `MatchingStrategy` do.
- Value objects (`TimeSlot`, `MatchScore`) are immutable. No setters.
- No magic numbers. Weights and thresholds come from configuration, never from a constant
  buried in a scorer.
- Constructor injection only. No field `@Autowired`.

## Configuration and secrets

- Anything tunable lives in `application.yml`, bound to a typed config object. Adding a key
  means adding its row to [docs/CONFIGURATION.md](docs/CONFIGURATION.md) in the same change.
- Backend secrets go in `backend/src/main/resources/application-local.yml`. It is git-ignored,
  plain YAML with the same keys as `application.yml`, and loads automatically because
  `spring.profiles.active` defaults to `local`. Create it yourself; no template is committed.
  There is no `backend/.env`: Spring Boot does not read `.env` files.
- Frontend settings go in `frontend/.env` (template: [`frontend/.env.example`](frontend/.env.example)).
  Anything prefixed `VITE_` ends up in the public bundle, so never put a secret there.
- Never write a real credential into a tracked file: not `application.yml`, not a test, not a
  comment, not a "temporary" default.
- If you think a secret was committed, say so immediately. Rotating it is easy, but only if we
  know.

## Frontend conventions

- Function components with hooks. No class components.
- API calls go through the shared client, which reads `VITE_API_BASE_URL`. Never hard-code
  `localhost:8080` in a component.
- Components in `PascalCase`, one per file, file named after the component.
- Screens and their API and form helpers live in `src/features/<feature>/`; the client, auth,
  hooks and styles in `src/shared/`; reusable UI in `src/shared/components/`. Routes live in
  `src/routes.tsx`; the router is created once in `src/main.tsx`.
- **Reuse the shared components** (`AppShell`, `RetroWindow`, `Button`, `Field`, `Dialog`,
  `StatePanel` and the rest, listed in the [UI guide](docs/UI_GUIDE.md#shared-components)). A
  one-off second button is how a design system dies. Say so when you add a shared component.

### Visual direction

Pixelated, retro, desktop operating system: decorative window title bars, pixel icons and
chunky bevelled buttons, with conventional routes and no movable window manager. Text uses
Tahoma, Verdana and system fonts; no web font is loaded. Figma is the source of truth and wins
over this file; if a screen is not in Figma, ask before inventing one. The Figma link, the
implemented choices and what is still to check are in the [UI guide](docs/UI_GUIDE.md).

### Rules for every screen

- **Every list has an empty state.** An unexplained empty table looks like a bug in a demo.
- **Every async view has a loading state and an error state.** The demo runs on SMU wifi.
- **Nothing sensitive is hidden with CSS.** The backend withholds a contact number, or nobody does.
- **Buttons say what they do.** "Send match request", not "Submit".
- **Destructive actions confirm first:** leaving a group, removing a member, deleting an account.
- **Forms validate before submitting** and say what is wrong next to the field that is wrong.

## Testing

- Unit-test anything with logic. Scoring and interval maths are pure functions with no excuse.
- Handle and test the empty, invalid and unauthorised cases: a student with no availability, a
  match with no shared courses, a request to view a profile you are not connected to.
- Keep the context-load test (`StudyBuddyApplicationTests`) passing. It fails when a bean
  cannot be wired, a config property is missing or an entity mapping is malformed.
- Run the [commands](#commands) before you push. Strategy and coverage by area are in the
  [testing guide](docs/TESTING.md).

## Settled decisions

Build on these; do not reopen them. Reasons: [design decisions](docs/DESIGN_DECISIONS.md).

- **Preferred group size (Team A).** Students choose one-to-one, small group or either, stored
  as a minimum and maximum number of people. `GroupSizeScorer` scores interval overlap, as
  Appendix A does, and the search filter keeps students whose range overlaps the chosen
  category. See the [matching engine](README.md#matching-engine).
- **Deactivation vs deletion (Team C).** Deactivate is reversible: it revokes sign-in, ends
  active connections and group memberships, and keeps history. Delete is permanent and removes
  the account and its dependent records.
- **Basic usage information (Team C).** Last successful sign-in, number of active connections,
  and number of open groups the student belongs to, including ones they lead.
- **Window chrome.** Decorative title bars with conventional routes and shared retro primitives.
- **Learning style and contact format (team).** The defined study preferences cover the brief's
  undefined learning-style wording. Contacts accept Singapore or international formats with
  shared normalisation ([baseline contract](docs/DESIGN_DECISIONS.md#baseline-requirement-contract)).

## Team ownership

Stay in your lane. Do not refactor another team's code as a side effect of your task; if it
blocks you, raise it rather than rewriting it. Shared files (`application.yml`, `pom.xml`,
`package.json`, the routing table, the shared API client) are edited with care and flagged in
the pull request description.

| Team | Members | Owns |
| ---- | ------- | ---- |
| Team A | nat, cy, jo | Matching engine: scorers, `TimeSlot` maths, strategies, `MatchScore`, matching config, matching and admin-config UI; the Study Room tab (personal Pomodoro and groups panel) and its music (`music` package, Spotify and Freesound) |
| Team B | angel, averyl, gh | Spring Boot and React skeletons, entities, enums, repositories, Spring Security and JWT, `ProfileService`, data seeder, shared UI shell, auth and profile screens; AI study tools: `ai` client and quota, `quiz`, `studyplan` (personal and session roadmaps), `assistant` (Pixel) and their screens |
| Team C | aryan, charlize | `MatchRequest` and its state machine, `Connection` (including ending one), `ProfileViewAssembler` and the profile DTOs, `StudyGroup`, `GroupMembership` and group join requests, admin users, notifications, and their screens; E1 match explanations (`matchexplanation`); E2 group rooms (`studyroom`, including room reactions), `studysession` and `weather`, `achievement` and the focus forest (`forest`); help guide |

## Git workflow

- **Say what "done" means** before you start (which behaviours, which edge cases) and again
  in the pull request.
- **Never commit to `main`.** Branch from an up-to-date `main` as `type/team/short-description`
  (`feat`, `fix`, `test`, `docs` or `chore`; `team-a`, `team-b` or `team-c`), for example
  `feat/team-c/match-request-state-machine`. Omit the team on `docs` and `chore` branches that
  belong to nobody in particular.
- **Commits are small and do one thing.** Present tense, lower case after the colon, no full
  stop: `fix: reject match request to an already connected student`.
- **No AI attribution in commits or pull requests.** No `Co-Authored-By` trailer for an
  assistant, no "Generated with" footer, nothing similar. The work is submitted as ours and
  the history should read that way.
- **AI use is disclosed instead** in [docs/AI_USAGE.md](docs/AI_USAGE.md) and the presentation.
  Record retained AI planning, code, review and documentation help accurately; never omit
  required disclosure or invent human review.
- **Pull requests** go into `main` and say what changed, what it relates to, how it was tested
  ("it compiles" does not count) and that the checks hold: no logic in the controller, no
  entity returned to the API, both authors can explain every line, and the empty, invalid and
  unauthorised cases are handled. Get CI green before asking for review.
- **Review and merge.** One reviewer from another team, then **Squash and merge** and delete
  the branch.

Milestones and review etiquette are in the [team workflow](docs/TEAM_WORKFLOW.md).

## Do not

- Do not guess at a graded requirement. If it is ambiguous, stop and ask; guessing produces
  features nobody asked for.
- Do not scaffold or modify `pom.xml` or `package.json` unless the task is explicitly that.
- Do not commit `.env`, `application-local.yml`, build output (`target/`, `dist/`,
  `node_modules/`) or IDE folders.
- Do not write code you cannot explain. Every author must defend every line in the viva.
- Do not leave a TODO in place of a requirement without flagging it in the pull request.
- Do not agree an endpoint with another team without recording it in
  [docs/API_CONTRACT.md](docs/API_CONTRACT.md).
