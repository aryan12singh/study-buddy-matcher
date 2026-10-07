# Team C pull request into main

Published as [PR #48](https://github.com/aryan12singh/study-buddy-matcher/pull/48).
This is the concise repository summary; the published PR contains the detailed review map.
Human review and squash merge remain outstanding.

**Title:** `feat: complete team c requests, groups, notifications and admin`

## Description

Team C's service layer previously lacked authenticated HTTP routes and usable React screens;
duplicate decisions/capacity races and account lifecycle cleanup also needed persisted
protection. This change completes requests/connections, private/public study profiles,
study groups and membership decisions, notifications, and admin create/edit/deactivate/
reactivate/permanent-delete behavior end to end.

Requests preserve course/goal context and reveal contact only after acceptance. Disconnect
revokes future contact reads. Group approval/edit/close use the same capacity lock, and
notifications commit with domain transitions. Deactivation revokes existing JWTs and ends
active relationships while retaining history; permanent deletion separately removes the
account and dependencies, including led groups. Self/last-admin removal is blocked under
concurrent operations. Admin usage and last-login data are real; contact input is write-only.

Relates to [#10](https://github.com/aryan12singh/study-buddy-matcher/issues/10),
[#11](https://github.com/aryan12singh/study-buddy-matcher/issues/11),
[#12](https://github.com/aryan12singh/study-buddy-matcher/issues/12),
[#13](https://github.com/aryan12singh/study-buddy-matcher/issues/13) and
[#14](https://github.com/aryan12singh/study-buddy-matcher/issues/14).

### Shared files and database rollout

Approved bounded foundations provide login/registration/JWT/current principal, course and
activity reads, opt-in synthetic seed, a shared API/auth client, routing and retro UI
primitives. These must be reconciled with B's integration without duplicate implementations;
A's matching code and B's full own-profile/dashboard product features are not claimed here.

Shared changes include User/Student queries, application configuration, auth/security/common/
seed, frontend App/main/auth entry points/shared primitives, .env.example, CI and docs.
The existing Maven JAR plugin excludes private local YAML from distribution. The npm lock
patch updates only existing transitive source-map-js to 1.2.2; package.json/direct dependencies
are unchanged. No new runtime/test library was added.

Four ordered SQL files install the baseline, UTC legacy event conversion, account/context
fields, unique/check constraints/indexes, backend-only RLS/grants and goal collection keys.
Normal startup validates rather than auto-updating schema. The reviewed shared Supabase
upgrade is already applied, and its safe seed has 50 students/10 courses. Fresh environments
must migrate before startup; existing populated schemas need the documented preflight and
private backup. No shared truncate/reset was used. See DATABASE_OPERATIONS.md for history,
access-policy notices and recovery.

### Validation

- 228 backend tests passed on fresh PostgreSQL 17.11, including 34 real HTTP/database tests,
  separate-transaction races, literal contact absence, role/token checks and rollback.
- Legacy schema upgrade, duplicate-email preflight rollback, UTC/weekly-slot preservation
  and repeated migrations passed in a separate disposable database.
- 108 frontend tests across 16 suites passed on Node 22; lint has zero warnings/errors,
  TypeScript/production build passed and npm audit reported zero vulnerabilities.
- Three browser sessions exercised real student request/accept/disconnect, group create/
  edit/apply/approve/reject/remove/close, agenda, notification read/filter and admin flows.
  Restart/reload preserved saved fields, history/read state and edits. Permanent deletion
  and revoked access were also verified over real HTTP.
- Shared JDBC startup/auth reads and a second seed startup passed without identity/hash
  changes. All 16 tables have RLS and no browser table grants; actual role denial was tested
  locally. Both distributable JARs exclude application-local.yml.
- The final review found three P2 issues. All are fixed with
  regressions: concurrent login/email edit, refresh-retained request drafts, and recipient
  direction on withdrawal notification links. Disposition is in TEAM_C_TESTING.md.

The initial [hosted CI run](https://github.com/aryan12singh/study-buddy-matcher/actions/runs/37547938687)
passed both jobs at `d0270cf`; [PR checks](https://github.com/aryan12singh/study-buddy-matcher/pull/48/checks)
show the latest pushed commit. Another team's human review and
both C authors' code walkthrough remain required before merge. Controllers use services,
API services return assembled DTOs, and empty/invalid/unauthorized/error states are covered.

The approved follow-up fixes late-save navigation and refresh-discarded admin action state,
including permanent-delete confirmation/current-email checks. A shared `useViewNavigation`
guard was the added frontend foundation in that follow-up. Group summaries and notification links use batch
queries without changing API responses: browse/own lists use 7 queries across the measured
small/large fixtures, and notifications use 5. Regression details and original failure counts
are in TEAM_C_TESTING.md. No dependency, migration or A/B matching/profile feature was added.

### Approved UI polish

All eight approved improvements and retro delighters are complete: consistent pixel
icons/initials and hierarchy, compact responsive shell/account cards, destination save/delete
messages, URL tabs/filter chips/counts/empty actions, capacity meters, weekly copy/duplicate/
sorted preview, canonical group/contact copy with fallback, and dirty/pending draft guards
with sticky actions. Copy contact still relies on the current self/CONNECTED profile DTO;
actual acceptance/disconnect browser checks verified visibility and immediate removal.

`routes.tsx` now exports the same guarded route paths; `main.tsx` creates the existing
React Router data router once and `App` supplies auth/RouterProvider. This supports official
Back/sidebar/reload blocking. A/B should add routes to this tree and reuse the new shared
primitives, rather than mount a second router. Form and request/application dialogs retain
inputs until explicit discard, while late completions refresh data without changing newer
views. Generic destination messages are consumed once; private drafts never enter history.

Native browser verification used synthetic accounts against a disposable PostgreSQL/Java
backend: three weekly slots persisted, membership approval updated 2/4 capacity, clipboard
success/denial worked, admin create/edit/deactivate/reactivate/permanent deletion completed
through the UI, narrow cards retained actions, and reduced motion removed transitions.
The pass added 31 frontend tests (108 total across 16 suites), reran all 228 backend tests,
and passed a fresh Node 22 locked install/test/lint/build/audit. No new backend endpoint/DTO,
schema, configuration, dependency, external icon/font or A/B product feature was needed.
[UI acceptance/integration/screenshots](TEAM_C_UI_POLISH.md) gives the full evidence.

### Review references

Read docs/API_CONTRACT.md, docs/TEAM_C_REQUIREMENT_COVERAGE.md, docs/TEAM_C_TESTING.md,
docs/TEAM_C_HANDOFF.md and docs/diagrams/TEAM_C.md. The handoff includes shared integration
steps and a short demo/viva walkthrough. No bonus timer/rooms/calendar/AI feature is in
this branch; extras follow after core A/B integration on separate branches.
