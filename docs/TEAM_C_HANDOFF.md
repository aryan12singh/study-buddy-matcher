# Team C handoff and demo

Team C development covers all five compulsory issues (#10–14): buddy requests and ending
connections, backend contact privacy, study groups/membership, notifications and account
administration. [Coverage](TEAM_C_REQUIREMENT_COVERAGE.md), [verification](TEAM_C_TESTING.md),
[API contract](API_CONTRACT.md), [decisions](DESIGN_DECISIONS.md) and
[diagrams](diagrams/TEAM_C.md) describe the finished interfaces and evidence.

This branch still needs authorized publication, hosted CI and another team's human review
before squash merge. An automated review does not establish that Aryan and Charlize can
explain every line; the walkthrough below is their preparation checklist.

## Integrating Team A and B

The approved exception supplied missing auth, seed, course-listing, activity counts and UI
foundations so C could run end to end. Reuse these boundaries or deliberately reconcile
them with the corresponding B implementation; do not mount two providers/clients/guards or
maintain two JWT formats. Matching code was not rewritten by this implementation.

| Consumer | Integration boundary | Action |
| --- | --- | --- |
| B authentication / all screens | auth/login, auth/register, auth/me; AccountPrincipal; AuthProvider/useAuth/RequireRole | Preserve account ID/role/token-version validation, normalized email and 401 session expiry |
| A matching candidates | AccountAccess eligibleStudent/eligibleStudentIds; active student query | Exclude inactive/missing/non-student identities; obtain actor from principal |
| A matching cards | SendRequestDialog, sendMatchRequest; `/students/:id` | Pass `origin: MATCHING` plus known course/goal, then refresh real resources |
| B dashboard | GET `/api/students/me/summary` | Use activeConnections, pendingIncoming, pendingOutgoing and acceptedGroups; do not label active buddies as online |
| Alternate dashboard consumers | getRelationshipCounts in connections/api.ts | Provides pending incoming/outgoing and active connection counts using the supported list endpoints |
| B own-profile/preferences editor | Same Student/User mapping, enum values and Public/ConnectedProfileDto fields | Implement the separate own-profile/availability API; C's self-view is a read, not that editor |
| Any course selection | GET `/api/courses`, shared courses.ts | Student-authenticated course-code-ordered DTOs, no entity serialization |
| All C screens | shared/api/client.ts and useResource/useAction | VITE_API_BASE_URL; safe problems/fieldErrors; abort stale work; refresh after mutations |
| Shared shell | AppShell, WindowPage, Button, Field, Dialog/ConfirmDialog, TabBar, StatePanel, ActionNotice, Badge, WeeklySchedule/WeeklySlotEditor | Reuse existing primitives and decorative chrome; do not duplicate them per screen |
| Schema / all backend writers | Ordered SQL, AccountAccess and DatabaseMutationLock | New writes must follow the shared account-lifecycle guard and sorted account → group/request lock order |

Student and Admin are the only account roles. A group leader is its `leaderId` relationship.
Self and an active accepted buddy may read contact; a group peer/leader and an administrator
may not. The admin edit form deliberately receives no contact to prefill: a blank replacement
omits it and preserves the saved private value. Ordinary edits do not change roles/passwords.

Services return DTOs assembled within the transaction. Internal eligibility collaborators
can return domain objects to other services; these are not controller/API return values.
Controllers inject services only. Domain transitions and their required notifications commit
or roll back together. Repeated decisions conflict instead of duplicating events.

### Frontend refresh and session behavior

The API client accepts a server origin or its `/api` prefix. Tokens live in tab-scoped
session storage; profile payloads are held only in mounted resource state. 401 clears the
current token/session, protected routes unmount and resources are purged. Old responses
cannot repopulate a new account/view after cancellation. Disconnect explicitly purges
private profile data before refetching.

Successful mutations dispatch `resources-changed`. Read views refresh on focus, visibility
and every 30 seconds by default (15–300 seconds configurable); hidden tabs do not poll.
Edit/create forms do not refresh over unsaved edits. Another user's mutation becomes visible
on reload/focus or the next refresh, rather than via an invented WebSocket/live-presence API.
The backend remains authoritative for permission and eligibility after waiting for locks.

## Shared files and integration exceptions to review

| File/area | Reason |
| --- | --- |
| User/UserRepository; StudentRepository; StudyBuddyApplication | Last successful login and token version, locking/active identity reads, typed-config discovery |
| auth/, security/, common/, seed/ and course/activity reads | Minimum missing identity/role/error/seed/consumer foundation |
| application.yml and application-test.yml | Typed auth/CORS/seed settings, UTC JDBC, validation-only schema and isolated tests |
| Four SQL migrations; scripts/migrate-database.sh | Reproducible upgrade/uniqueness/checks/indexes/timestamps; deny-all browser database access |
| scripts/test-backend.sh, test-migrations.sh and synthetic SQL fixtures | Ephemeral PostgreSQL tests, old-schema upgrade/repeat/preflight rollback |
| backend/pom.xml | Existing inherited JAR-plugin configuration excludes private application-local.yml; no dependency added |
| Frontend App/main, auth entry points and narrow landing auth links | Working provider/routing/guards and entry to C screens |
| Frontend shared/ and .env.example | One API/auth/refetch boundary and shared retro primitives |
| frontend/package-lock.json | Existing transitive source-map-js 1.2.1 → 1.2.2 security patch; direct dependencies/package.json unchanged |
| .github/workflows/ci.yml | PostgreSQL-backed backend checks and Node 22 frontend tests/lint/build on every PR |
| README, AGENTS and docs | Settled C policies, configuration/library inventory, current scope and verification |

Ignored backend/local/test/demo configs and schema snapshots are private operational files.
They are absent from Git and the distributable JAR. Do not attach them to the PR or slides.
The pre-existing untracked metadata helper was preserved and is not part of this change.

## Demo preparation

Apply the schema for a fresh database, provide private local config, install the locked
frontend packages and follow [README](../README.md). Shared Supabase already contains the
upgraded schema, ten courses and fifty synthetic students. Demo passwords came from private
runtime seed configuration, with no default committed password. This implementation created
an ignored `.env.demo.local` holding them for the local operator; use it privately, and leave
seeding disabled during normal demonstrations. A deliberately enabled seed can recreate a
previously deleted demo identity, so it is unsuitable for a deletion demo restart.

Use separate browser sessions for Priya (`priya@demo.example.test`), Jamie
(`jamie@demo.example.test`) and `admin@demo.example.test`. Student IDs are obtained from
responses, not assumed from the seed's insertion order. Use a disposable local account for
permanent-deletion demonstrations, rather than deleting shared teammates/demo owners.

Suggested C segment within the team's presentation:

1. Priya opens Jamie's public profile and sends a contextual buddy request. Jamie sees its
   notification/incoming card, accepts, and Priya reloads to show backend-granted contact.
2. Priya disconnects with confirmation. Show the subsequent public JSON has no contact field.
3. Priya creates a course group with goals, mode, capacity and weekly schedule. Jamie browses,
   applies, and Priya approves. Show the leader counted once and no group contact disclosure.
4. Show saved agenda, applicant rejection/member removal and closed-group behavior. Explain
   a real two-transaction race test for one remaining seat instead of relying on click timing.
5. Mark a notification/read-all and reload. Show honest zero/newest-first/filter behavior.
6. Admin shows actual usage/last login and creates/edits a disposable student. Explain separate
   deactivate/reactivate/delete, retained history versus cleanup, old-token rejection and
   self/last-admin protection. Use isolated fixtures for the irreversible demonstration.

### Walkthrough and viva prompts

- Explain `@MapsId` and why Student composes User; explain why leader is not a third role.
- Trace controller → service → domain/repository → assembler → DTO for a request acceptance.
- Point to PublicProfileDto versus ConnectedProfileDto and the actual absence assertion.
- Explain pending-request and active-connection unordered partial indexes, sorted row locks
  and why a check before a write alone cannot prevent two acceptances.
- Explain the shared/exclusive PostgreSQL advisory guard around account cleanup, transaction
  rollback and cross-instance correctness; new B/A writes must follow it too.
- Trace capacity approval/edit/close through the same group row lock and revalidation.
- Explain token version on deactivation, last successful login and immutable account roles.
- Trace StudentDeactivation versus StudentDeletion, survivor events and removed resource links.
- Explain UTC event instants versus recurring Singapore weekly slots and the legacy upgrade.
- Run the isolated suite, inspect a privacy/race/rollback assertion and reproduce the browser flow.

## What remains outside C development

Publish the prepared PR when authorized, obtain another team's human review, run hosted CI
and squash merge before closing issues. Integrate A's matching/admin-config UI and B's full
profile/preferences/availability/dashboard product features through the boundaries above.
Resolve the original shared landing/favicon/icon provenance, team-wide slides and rehearsal,
and individual peer evaluation before submission. Existing landing footer Privacy/Contact
placeholder links remain in the shared entry-page scope. No core C behavior is left as a TODO.

After core integration, put bonus rooms/timer/music/AI explanations/dated sessions on separate
branches, each with its own agreed behavior, API and tests. Do not confuse the saved weekly
agenda with a promised calendar or session-planning product.
