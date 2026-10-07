# Team C implementation and completion plan

Status: **approved by Aryan on 7 October 2026; implementation, final-review fixes and approved UI polish complete**.

This records the approved implementation scope. Actual completion evidence is in
[TEAM_C_TESTING.md](TEAM_C_TESTING.md) and [TEAM_C_REQUIREMENT_COVERAGE.md](TEAM_C_REQUIREMENT_COVERAGE.md):
228 backend tests and 108 frontend tests after the approved follow-up and UI polish, with rerun lint/build
and migration checks. Earlier implementation evidence also covers packaging, real browser
and restart journeys, shared schema/seed verification, and all three Astra findings
fixed. [PR #48](https://github.com/aryan12singh/study-buddy-matcher/pull/48) is published and
initial hosted CI passed. Human cross-team review, both owners' walkthrough, latest PR CI
verification and merge remain release responsibilities.
Approval authorises the described application changes, the bounded shared foundations,
the proposed account policy and screen layouts, and the tested database upgrades described
below. Aryan subsequently authorised clean commits, PR publication and the additional audit
fixes/list-query batching, then the full eight-item UI polish and retro delighters.
The polish is implemented and verified in [TEAM_C_UI_POLISH.md](TEAM_C_UI_POLISH.md);
its flows use the existing authenticated backend without a schema or API change.
Merge remains a separate release action.

## 1. Objective and scope

Complete Team C's compulsory features on `feat/team-c/match-request-state-machine`:

| Area | Issue | Completion means |
| --- | --- | --- |
| Buddy requests and connections | [#10](https://github.com/aryan12singh/study-buddy-matcher/issues/10) | Send, list, accept, decline and disconnect through real authenticated APIs and screens; safe under concurrent requests |
| Other-student profiles and contact privacy | [#11](https://github.com/aryan12singh/study-buddy-matcher/issues/11) | Public study profile; contact returned only to self or an active buddy; no alternate data path bypasses this rule |
| Study groups and membership requests | [#12](https://github.com/aryan12singh/study-buddy-matcher/issues/12) | Browse, Your groups, details, agenda, create, edit, approve/reject, remove and close; correct permissions and capacity |
| In-app notifications | [#13](https://github.com/aryan12singh/study-buddy-matcher/issues/13) | Correct events, links, filters, unread counts and persisted read state |
| Admin user accounts | [#14](https://github.com/aryan12singh/study-buddy-matcher/issues/14) | Create, read, update, deactivate, reactivate and separately delete; real usage data and last-admin protection |

Implement the minimum missing Team B foundations needed to make these features work, as
authorised in this conversation. Reuse Team B implementations if they become available;
do not maintain competing implementations. Shared changes are listed in section 10 and
will be flagged in the handoff.

Team A retains scoring, matching strategies, ranking/filter UI and admin matching settings.
Team B retains the full own-profile/preferences editor and dashboard product scope. Team C
provides the real counts, actions and public DTO contracts those consumers need. This plan
includes a bounded auth/data/shell foundation if it is still absent; it does not redesign the
other teams' features.

Bonus work stays on later branches. No study rooms, Pomodoro/music, AI helper, dated sessions,
group ranking, richer task boards, global online status, self-disable or window manager is
part of this branch. Active connections are accepted relationships, not online presence.

## 2. Verified starting point and gaps

- Current branch: `feat/team-c/match-request-state-machine`, audited at `fbd6929`;
  `origin/main` was `cda8ecc`, with this branch 18 commits ahead and none behind.
- Existing Team C entities, services, assemblers and unit tests are a useful base. They
  are not yet exposed by Team C controllers or a working JWT/current-user integration.
- The audited backend run passed 163 tests using the existing local Supabase configuration.
  This is not proof of HTTP security or persisted concurrent correctness: most tests use
  mocks, and the context test depends on the private local datasource.
- Frontend build and lint passed; `npm test` had no tests and failed. Auth pages are
  placeholders and all six Team C screen areas are absent.
- Clean CI lacks a database/configuration for the context test.
- Pending buddy requests, active buddy connections and pending group requests lack
  database uniqueness protection. Acceptances and group capacity changes lack the locks
  needed to make check-and-write operations atomic.
- An inactive recipient can currently receive a buddy request. Status and relationship
  eligibility need consistent enforcement throughout the services and authentication.
- Database string widths and input validation are not aligned. Generated notification
  summaries can also exceed their current 255-character database column.
- Admin creation is absent; the edit response omits needed profile fields. Last login,
  last usable admin protection and a separate permanent-delete operation are absent.
- Request context, notification resource links/filtering, Your groups and an applicant's
  own group-request view need contract and implementation work.
- The audited Supabase database had 16 public tables, no demo records, disabled RLS and
  browser-role access to sensitive tables. Backend privacy alone cannot protect that path.
- Preserve the pre-existing untracked `setup_study_buddy_metadata_reviewed.py`; do not
  execute it or fold it into this feature as an unrelated change.

Recheck checkout, open PRs, shared foundations and database state before implementation.
Do not reset local changes or assume the audit snapshot remains current.

## 3. Decisions proposed for approval

### Account lifecycle

The formal assignment says administrators must create, update and delete accounts, and
view status/basic usage. It does not specify hard versus soft deletion. `AGENTS.md` assigns
that decision to Team C. The earlier API proposal treats Delete as Deactivate; the current
[#14](https://github.com/aryan12singh/study-buddy-matcher/issues/14) explicitly requires
separate operations. Replace the earlier proposal with:

| Operation | Account | Related data | Reversible? |
| --- | --- | --- | --- |
| Deactivate | Keep the account/profile, set inactive and invalidate access | End active connections; decline pending buddy requests in both directions; close led groups and reject their pending applicants; reject the student's pending group applications; remove their memberships. Retain answered history and closed groups | Account access can be reactivated |
| Reactivate | Set active and permit a fresh login | Do not restore ended connections, declined requests, memberships or closed groups | Yes |
| Delete permanently | Remove the account and student profile | Remove that student's requests, connections, memberships, availability, enrolment/goal rows and recipient notifications. Remove groups they lead, including those groups' dependent rows; notify remaining affected users. Remove notifications linked to deleted records or replace them with a safe generic event without a broken target | No |

Groups led by a permanently deleted student are removed rather than transferred to another
leader or retained with an orphaned leader. Groups led by someone else remain; only the
deleted student's membership/applications are removed. Normal group closure and account
deactivation retain a clearly closed group record. The delete confirmation must state
exactly what will be removed. All cleanup and required notifications form one transaction.

Block self-deactivation/self-deletion and removal of the last usable administrator. Serialize
administrator removal/status changes in a fixed lock order and recount active administrators
after locking; a count performed before the write is insufficient. An administrator whose
own access was concurrently revoked cannot finish a privileged mutation.

Reactivation must not revive a JWT issued before deactivation. Include an account token
version/revocation value alongside the active-account check, increment it on deactivation,
and require a fresh login after reactivation. Deletion makes all existing tokens invalid.

Account role is selected and validated at creation. It is read-only during ordinary account
editing; Student/Admin conversions and password-reset workflows are not added as implicit
requirements. Public registration, if needed for the missing auth foundation, creates only
students and cannot supply an elevated role.

Use one trimmed, case-normalised email identity for login, registration and admin edits,
with database-enforced uniqueness. Inactive accounts retain their email; permanent deletion
releases it. Reconcile existing case-duplicate identities during migration preflight rather
than silently choosing an account to remove.

### Usage and contact data

- Usage includes **last successful login**, active connection count, and accepted group
  count. Define accepted group count as current membership in open groups, including
  groups led by the student. Display "Never logged in" and zero counts honestly.
- Keep useful existing request/history/group-led counts if displayed, but label their
  definitions explicitly. Admin account summaries/counts are backed by actual data.
- Record last login only after successful authentication, not on failed login or token use.
- Student contact visibility remains self or an active accepted buddy. Shared group
  membership and leadership do not grant contact access.
- Preserve the current **no contact number in admin responses** policy. Admin creation
  can accept a contact number; editing can supply a replacement. An omitted replacement
  preserves the existing number, so the form can work without reading private contact data.
  Return the other editable student fields for form prefill. Passwords/hashes/tokens never
  appear in account management responses or ordinary logs.

### Group and request rules

- Leader is a group-specific relationship and the first accepted member, not an account role.
- Leader counts toward capacity; pending requests do not. Minimum maximum-size is two.
- Capacity reduction below accepted membership is invalid. Approval, membership removal,
  capacity edits and closure use the same group locking protocol.
- Buddy and group requests remain separate state machines. Repeated decisions return a
  documented conflict and create no duplicate connection, membership or notification.
- Normal closure is one-way. A leader cannot be removed. Student self-leave and leader
  transfer are not introduced by this plan.
- All group fields are carried through the form, DTO, database and read views: name,
  description, course, goals, meeting mode, weekly availability and maximum size. Preserve
  existing optional-value rules where the brief does not define a minimum; show an honest
  empty schedule/goals state. Record exact requiredness and limits in the API contract.
- Store structured buddy-request context: origin, optional course and optional selected
  study goal, using existing domain enums/identifiers. Matching-origin requests carry the
  known search context; profile-origin requests remain possible without inventing a course.
  Validate any supplied context and return it with the request for display/history.
- Use UTC instants with explicit offsets in API timestamps, and `Asia/Singapore` for weekly
  schedules and Today/Earlier notification grouping, labelled in the UI. Document/configure
  the campus timezone. Interpret existing offset-free timestamps according to their
  documented origin during migration; do not silently relabel them as UTC. Weekly slots
  retain their campus-local meaning without rewriting Team A's interval maths.

### Screen direction

[Figma OOPs](https://www.figma.com/design/OPbwyiH0iyinAnJl5zKalm/OOPs) remains the source
of truth. Inspected image nodes `30:35` and `30:38` contain retro reference wireframes for
profiles, connections, notifications and group detail, not finished editable designs for
every screen. Complete admin and group-management layouts are missing.

Approval of this plan includes the concrete missing-layout proposals in section 7. Use
decorative window frames with conventional routing, readable body text, existing/system
fonts and simple existing or authored icons. Do not ship draggable/stacked windows, fake
online tabs, misleading window-control buttons or simulated loading percentages. Use
existing tokens where compatible with Figma; avoid a broad landing-page/style rewrite.
New Figma designs supersede these proposals when integrated.

## 4. Stage A — contracts, shared backend foundations and reproducible setup

1. Update `docs/API_CONTRACT.md` with the approved C DTOs, actions, filters, errors and
   identity rules before implementing either side. Mark proposals and implemented/verified
   rows truthfully; do not claim cross-team agreement or a merged PR that has not happened.
2. Add a small current-user/authentication boundary if Team B has not supplied it: existing
   Spring Security/JJWT, constructor-injected token service/filter, validated typed config,
   PasswordEncoder and an authenticated principal exposing account ID and role. Verify the
   current account's active/existing status and token version on protected requests.
3. Provide working login and current-account endpoints. If missing, provide the minimal
   student registration needed by the existing auth entry point, sharing the same password,
   email-normalisation and role rules with admin creation. Keep full profile/preferences
   CRUD with Team B. Use explicit role rules, configured CORS and stateless API security.
4. Provide a course-listing service/DTO/controller and reusable active-student eligibility
   query/policy. Team A consumes that boundary; do not rewrite or invent a matching engine.
5. Add safe shared error handling: stable problem codes, field-validation errors and
   consistent 400/401/403/404/409 responses. Controllers obtain actor identity from the
   principal, never from caller-supplied acting-user/admin IDs.
6. Provide isolated PostgreSQL 17 locally using the existing Docker tooling, plus a fresh
   PostgreSQL service in CI. Keep test configuration separate from `application-local.yml`.
   Do not run integration/race fixtures against the shared Supabase database.
7. Add a versioned SQL baseline and upgrade files using existing SQL/psql/Supabase tools.
   Apply the same schema/constraints to isolated tests and the shared environment after
   preflight and validation. Normal startup validates schema instead of relying on Hibernate
   auto-update to create privacy/uniqueness guarantees. Document how to adopt an existing
   schema without resetting its data.
8. Reuse Team B's seeder if available. Otherwise supply a small, opt-in, idempotent demo
   bootstrap for the required 10 distinct courses and 50 synthetic student profiles, plus
   administrator and named Team C scenario identities. Seed passwords come from runtime
   configuration. Re-running must not duplicate records, overwrite personal accounts or
   erase shared data. Tests have independent fixtures.

No new application dependency, hosted service, icon library or font package is planned.
Use the dependencies already present. Document existing runtime/test tools and libraries.
If a new dependency proves necessary, present that concrete change for agreement first.

**Exit check:** a clean environment can authenticate synthetic student/admin users, list
courses, start with the versioned schema and run a context/data test without personal secrets.

## 5. Stage B — finish and harden all Team C backend behaviour

### Buddy requests, connections and profiles

- Enforce active/existing actors and targets at the service boundary, including direct
  calls. Reject self-send, duplicate/reverse pending requests and active existing buddies.
- Add partial unique indexes for an unordered pending buddy pair, an unordered active
  connection pair, and a pending `(group, applicant)` pair. Historical answered requests
  and ended connections remain allowed. Inspect duplicates before upgrades; never
  arbitrarily discard existing shared data to make an index succeed.
- Lock and revalidate request state before accept/decline; create exactly one symmetric
  connection and the intended notification in the same transaction. Handle competing
  accept/decline, send/send, end/send and account-disable/delete races consistently.
- End a connection only for a participant. The next authorised profile lookup must return
  a public DTO to both sides, with no contact field/value. Enforce inactive/missing subject
  handling and no-store responses for private profile data.
- Extend profile responses with public weekly availability and caller-specific relationship
  state needed for correct actions: self, stranger, incoming/outgoing pending or connected.
  Keep public and connected DTOs distinct; assemblers determine privacy on every request.
- Expose safe request/connection counts and reusable send/profile actions for Team A's
  matching cards and Team B's dashboard. Their absence does not force C to fake data.

Partial uniqueness is supported by
[PostgreSQL partial indexes](https://www.postgresql.org/docs/17/indexes-partial.html).
Row locks must use a consistent order and recheck state after waiting; see
[PostgreSQL explicit locking](https://www.postgresql.org/docs/17/explicit-locking.html).
An in-process lock or a mock-only test is not sufficient for these guarantees.

### Groups and group requests

- Persist and return every group field and valid weekly slot; align field-size/enum/ID/time
  validation across service, controller and form. Do not silently drop schedule entries.
- Add Your groups and own group-application reads, including closed/history states.
  Return caller membership/request/leader state with details, so screens do not guess.
- Serialize approvals and all capacity-changing actions on the group row. Recheck active
  group, active applicant, request ownership/state, existing membership and available seats.
- Authorise management against the particular group's leader. Protect the leader, reject
  unrelated request IDs and preserve the membership uniqueness constraint.
- Closing prevents new applications/acceptances, rejects pending applications once and
  produces the appropriate notifications. Removing a member and deactivation/deletion
  refresh accurate counts and safe linked-resource states.

### Notifications

- Add safe related-resource type/ID and event identity/context. Links route to a permitted
  request, group or profile; unavailable targets produce a safe state rather than leaked data.
- Implement All/Requests/Groups filters, newest-first ordering, unread counts and idempotent
  mark-one/read-all for the current recipient only. Unread total is not accidentally changed
  by filtering the list.
- Keep one notification service, called within the successful domain transaction. Locked
  transitions/event identities prevent duplicate notifications on retry/concurrent actions.
- Define the expected recipient and number of notifications for send, accept, decline,
  disconnect, group application/decision, removal, closure and account cleanup. Do not
  generate messages containing stored contact data, hashes or tokens.
- Align summary storage with generated message lengths, using a text column where needed;
  do not let an otherwise valid domain action fail because a generated summary is too long.

### Admin accounts

- Implement student/admin account creation using the shared PasswordEncoder, email and
  validation rules; student identity/profile creation is atomic and respects `@MapsId`.
- Finish filtered list, real summary counts, detail and prefilled edits. Resolve the current
  DTO/form gap; an omitted contact replacement preserves the stored contact.
- Implement last-login and precisely defined usage counts, including null/zero states.
- Implement separate deactivate/reactivate/permanent-delete operations and the section 3
  relation policy. Reject self-removal, inactive actors and concurrent last-admin removal.
- Use explicit internal lifecycle operations for authorised account cleanup. Do not
  impersonate the student or call a public student action that now rejects the inactive
  account; keep cleanup authority separate from ordinary API permissions.
- Invalid/duplicate input or a failed cleanup/notification must roll back the whole action.
  Authentication and every C mutation must respect the final account state.

**Exit check:** all C business cases and saved-data/concurrency regressions pass against
isolated PostgreSQL; no controller is needed to compensate for unsafe service behaviour.

## 6. Stage C — real APIs, database access policy and HTTP tests

Add thin controllers over the existing services/assemblers. Every controller validates,
calls a service and returns a DTO/status. No repositories or query/business logic belong
in controllers. Use dedicated exception semantics rather than parsing message strings to
distinguish permission errors from state conflicts.

| API area | Required surface |
| --- | --- |
| Auth bridge | `POST /api/auth/login`, `GET /api/auth/me`; minimal `POST /api/auth/register` if missing |
| Course bridge | `GET /api/courses` |
| Buddy requests | `POST /api/match-requests`; incoming/outgoing `GET`; `POST /{id}/accept`, `POST /{id}/decline`; structured context in body/DTO |
| Connections | `GET /api/connections`; `DELETE /api/connections/{id}` |
| Study profile | `GET /api/students/{id}/profile`; public/connected shape, availability and viewer relationship state |
| Notifications | Filtered `GET /api/notifications`; `GET /unread-count`; `POST /{id}/read`; `POST /read-all` |
| Groups | Filtered `GET /api/groups`; `GET /api/groups/mine`; `POST /api/groups`; `GET/PUT /api/groups/{id}`; `POST /{id}/close`; `DELETE /{id}/members/{studentId}` |
| Group applications | Existing group-scoped create/list/accept/reject; `GET /api/group-join-requests/mine` for the current applicant's history |
| Admin accounts | Filtered `GET /api/admin/users`; `GET /api/admin/users/summary`; `POST /api/admin/users`; `GET/PUT/DELETE /api/admin/users/{id}`; separate `POST /{id}/deactivate` and `POST /{id}/reactivate` |

Document full DTOs, validation, filters, status codes, examples and permission rules for
these rows. Specify whether a denied case is 401, 403, 404 or 409 and test that contract.
The existing API has no consumers for the old Delete-as-deactivate proposal; update the
documentation and actual frontend together, and explicitly flag the semantic change.

Protect the alternate Supabase path before treating privacy as complete:

- Verify that all frontend database access uses Spring APIs with the application JWT.
- Enable RLS on exposed application tables with no browser allow policies, and revoke
  unneeded `anon`, `authenticated` and inherited/public privileges for this backend-only
  architecture. Preserve the intended backend JDBC access.
- Address future table/default grants so a later schema change does not restore browser
  access accidentally. Keep system-managed Supabase schemas outside this migration.
- Test direct anonymous/browser-role reads and writes as denied, and backend JDBC flows
  as functional. Re-run the Supabase security advisor and resolve applicable exposure
  findings; record any unrelated provider finding separately.

This follows [Supabase RLS documentation](https://supabase.com/docs/guides/database/postgres/row-level-security).
No service-role key or database password enters the frontend. Migration preflight, SQL,
verification and rollback notes must be reviewable and tested before shared changes.
Do not drop/reset/reseed shared tables. Destructive account tests use isolated fixtures.

**Exit check:** real JWT HTTP tests pass for students, admins, leaders, strangers, inactive
accounts, invalid tokens and missing resources; direct database access cannot bypass C privacy.

## 7. Stage D — shared frontend foundation and every Team C screen

First add/reuse one Axios client reading `VITE_API_BASE_URL`, typed DTOs, auth/session
provider, guarded routes, standard problem-error handling and mutation/refetch helpers.
Token storage follows one documented Team B-compatible policy; absent an existing policy,
use session storage for the token only, revalidate identity on app startup and clear it on
logout/unauthorised responses. Never persist contacts or complete profile responses there.

Build/reuse a small shared shell, window frame, labelled field/error, button variants,
tabs, dialog, empty/loading/error views and notification badge. Flag any new shared primitive.
Window chrome is decorative; controls that look actionable must have a real purpose or be
non-interactive decoration. Existing login/registration entry points become usable if the
bounded auth implementation is needed. Full own-profile editing remains with Team B.

| Screen and proposed route | Layout and supported behaviours | Backend support |
| --- | --- | --- |
| Requests/connections — `/connections` | Window with Incoming, Outgoing and Connected tabs; status/context/message/time; accept/decline; profile link; disconnect confirmation; honest empty states | Buddy request list/decisions, active connections/end, profile |
| Other student — `/students/:id` | Public identity/course/preferences/weekly schedule; hidden-contact guidance; optional-message request dialog; incoming/outgoing/connected state; contact only when supplied by the connected DTO; safe inactive/missing view | Profile/relationship DTO, request/connection actions |
| Notifications — `/notifications` plus shell badge | All/Requests/Groups tabs; Today/Earlier sections; unread indication; mark one/all; relevant action links; safe unavailable target | Filtered notifications, count, read/read-all and authorised target APIs |
| Groups browse — `/groups` | Course/goal/mode filters, accepted count/capacity and leader; Your groups and own application status/history; create and detail links | Browse, mine, own applications, course list |
| Group detail — `/groups/:id` | Course/goals/mode/description, accepted member names, leader, capacity, weekly schedule; request membership dialog; pending/member/full/closed states; View agenda popup showing saved goals/schedule | Group detail/viewer state, join request, safe member-profile links |
| Group create/edit — `/groups/new`, `/groups/:id/edit` | Window form for all fields, reusable weekly-slot editor and inline validation; saved values load on edit; back/cancel behaviour | Course list, create/get/update, leader check |
| Leader management — `/groups/:id/manage` | Detail header plus Applicants and Members tabs; approve/reject, capacity feedback, remove-member confirmation, edit and close confirmation; no remove-leader action | Leader request list/decisions, memberships/remove, edit/close |
| Admin users — `/admin/users` | Account table with search, role/status filters, real totals and status actions; create/detail/edit links; loading/empty/error views | Admin list/summary/create/status actions |
| Admin create/detail/edit — `/admin/users/new`, `/admin/users/:id`, `/admin/users/:id/edit` | Identity/role/status header, editable public profile fields, optional contact replacement, usage panel and last login; distinct deactivate/reactivate/delete controls; explicit irreversible-delete consequences | Admin create/get/update/usage/lifecycle |

These routes represent the six Team C feature areas; splitting detail/forms into routes
does not add bonus scope. The group-form/leader-management/admin layouts above are the
proposed layouts needing approval because Figma has no complete equivalents yet.

Requirements for every screen:

- Real API data and persistence; no mock counts, fake users, fake progress or dead controls.
- Loading, empty and error/retry states for each async list/detail/form. Invalid fields show
  inline errors; submissions/actions disable while pending and recover properly on failure.
- Confirm disconnect, member removal, group closure, account deactivation and deletion.
  Server permissions remain authoritative regardless of hidden/disabled controls.
- Refetch affected lists, profiles, membership/usage counts and unread badge after mutations.
  Refetch on focus and bounded background refresh where another user's actions affect the
  view. Document the refresh behaviour/configuration. Logout/401 discards private caches.
- Remove/refetch cached connected profiles on disconnect. Already received/saved contact
  data cannot be retroactively erased; the guarantee concerns later backend disclosure.
- Readable contrast/body text, keyboard operation, labels, focus restoration/trapping for
  dialogs, clear destructive controls, projector readability and a usable narrow layout.
  Document the browsers/viewports actually checked.
- Supply reusable request/profile actions and counts for matching/dashboard consumers.
  If those other-team screens are not present, verify the interfaces with contract tests
  and report that full-project consumer integration is still externally pending.

**Exit check:** each route performs its complete real backend journey, including invalid,
unauthorised, empty, loading and failure states. There is no required C button without an API.

## 8. Stage E — verification, CI and documentation

### Automated verification

Use the existing JUnit/Mockito/Spring test dependencies and Vitest/React Testing Library.
Do not add H2, Testcontainers, a browser-test dependency or a mock-server library silently.

| Test level | Required evidence |
| --- | --- |
| Domain/service | State transitions, eligibility, ownership, field limits, group capacity/leader invariants, usage definitions, account cleanup and notification recipients |
| PostgreSQL integration | Actual schema/unique constraints; two independent transactions for concurrent send/accept/approval/capacity-edit/close/account-disable/delete; last-admin race; one resulting connection/membership/event; transaction rollback on failures |
| HTTP/security | Real JWT login/principal handling, role checks, expired/missing/invalid tokens, inactive/deleted/pre-reactivation tokens, caller-ID tampering, invalid/duplicate identities, correct problem/status shapes |
| Privacy serialization | Literal contact field/value absent for stranger/pending/declined/disconnected/group-only peers; present only for self/active buddy; no stored contact/hash/token leaked through group/request/notification/admin payloads |
| Frontend interactions | Rendered loading/empty/error/retry states; request decisions; privacy states; group forms/capacity/permissions; notification filtering/read state; admin forms/lifecycle; confirmed destructive actions and logout cache clearing |
| Setup/CI | Fresh PostgreSQL schema and repeat migration/seed behaviour; context load without local secrets; backend suite; frontend test/lint/build; workflow changes also trigger their affected checks |

Concurrency tests must operate against persisted data with separate transactions and
assert final row counts/state/notifications. A barrier around mocks does not demonstrate
database correctness. Include maximum-length inputs and rollback tests for partial writes.

### Running-app and database verification

Use two separate student sessions and an admin session to verify:

1. Request send -> incoming notification -> accept -> contact reveal -> disconnect ->
   later profile payload public for both students. Repeat decline and invalid/duplicate cases.
2. Group create -> browse/filter -> apply -> approve/reject -> capacity/edit -> remove ->
   close. Verify saved fields/counts and member contact privacy throughout.
3. Notification links/filtering/read/read-all and correct unread counts after reload.
4. Admin create -> read/edit -> deactivate -> denied old-token access -> reactivate with
   fresh login -> permanent delete and dependency cleanup. Verify last-admin protection.
5. Reload and backend restart retain the saved state. Check actual network payloads, not
   just whether a field is visually hidden.
6. Direct Supabase browser-role access is denied; backend JDBC remains usable. Upgrade
   tests do not require destructive operations on shared data.

### CI

- Supply a healthy ephemeral PostgreSQL 17 service and generated/non-personal test config.
- Apply the versioned schema, then run backend tests with the explicit test profile.
- Run `npm ci`, `npm test`, `npm run lint` and `npm run build`; frontend tests must exist.
- Fix changed-area detection so relevant workflow/schema/test-tooling changes cannot skip
  the checks they affect. Keep runtime versions compatible with the existing fixed stack.
- Record local results separately from hosted CI. A green GitHub run is only claimed after
  a real run exists; publishing it needs the user's later commit/push authorisation.

### Documentation delivered with the implementation

- `docs/API_CONTRACT.md`: actual shapes, errors, privacy, identity, filters and lifecycle,
  including the changed Delete semantics and shared-foundation interfaces.
- `docs/DESIGN_DECISIONS.md`: approved account/usage/contact/leader/window/time decisions,
  reason and alternatives, owning teams, affected issues and compatibility notes.
- `docs/TEAM_C_REQUIREMENT_COVERAGE.md`: every C acceptance criterion mapped to its
  service, endpoint, screen and automated test/demo evidence; status and remaining
  external dependencies are explicit.
- `docs/TEAM_C_TESTING.md`: isolated setup, commands, concurrency/HTTP/browser cases,
  actual results, restart/persistence evidence and regression reproduction.
- `docs/DATABASE_OPERATIONS.md`: baseline/existing-schema upgrade, preflight, backup,
  SQL ordering, RLS/grants, safe opt-in seed, verification and rollback/recovery procedure.
- `docs/TEAM_C_HANDOFF.md`: reusable interfaces, shared files changed, Team A/B integration
  steps, demo accounts supplied through configuration, a concise demo script and code
  explanations/viva prompts for Aryan and Charlize.
- Team C class/ER and sequence diagrams for request acceptance/privacy, group approval,
  notifications and account cleanup. Describe the implemented composition model rather
  than copying the older inheritance proposal.
- `README.md`: correct current tree, clean setup/run/test/seed instructions, existing
  libraries/tooling and configuration tables, links to the above and honest feature status.
- `AGENTS.md`: reconcile settled C decisions, working shared primitives and the bounded
  foundation exception; preserve ownership and Figma precedence. Reconcile milestone
  references with the currently agreed project schedule rather than leaving contradictions.
- Contribution and required AI-use records describe actual work; do not invent human
  understanding/review or add AI-attribution trailers/footers to commits or PRs.

Update contracts and decision records as each phase lands; documentation is verified against
the final code instead of written once before the implementation and left to drift.

## 9. Stage F — requested model workflow and final acceptance

After plan approval:

1. Use **Sol at max reasoning** for implementation. If the intended Sol variant is not
   specified further, use `gpt-6.1-sol`. Give it this approved plan, repository instructions,
   current audit evidence and explicit file/ownership boundaries. Keep this work in the
   current task; no separate sidebar chat is needed.
2. Finish backend, APIs, screens, database/setup, tests and documentation. Track the coverage
   matrix and run the appropriate checks; do not stop after a partial feature batch.
3. Only once the whole implementation is ready, run **one final `gpt-6-astra` subagent at
   medium reasoning**. Its independent sweep checks the final diff and C acceptance
   matrix, security/privacy, concurrent behaviour, UI/API alignment, migrations, tests and
   docs. It is read-only and reports actionable findings with evidence.
4. Sol fixes every valid finding in the approved scope and reruns the affected checks.
   Track the disposition of all findings. Do not waive unresolved failures to label C done.
   No additional review-agent sweep is planned.
5. Deliver the final file/change summary, exact test results, migration/seed verification,
   browser evidence, coverage checklist and a concrete PR description/handoff. Aryan's later
   approval authorises clean local branch commits; push/PR publication and merge remain
   separately authorised release actions.

Team C development is complete only when:

- All five C issues' acceptance criteria have implementation and test/demo evidence.
- All six feature areas work through real authenticated backend APIs; every required action
  has loading/empty/invalid/unauthorised/error handling and saved-state verification.
- Concurrent actions preserve request/connection/member/capacity/last-admin invariants.
- Profile contact privacy holds through every applicable API and the Supabase access path.
- Account lifecycle, token revocation and dependency cleanup follow the approved policy.
- Local backend tests and frontend tests/lint/build pass with reproducible isolated config.
- Required schema and access-policy upgrades are applied and checked in the intended
  shared environment, with no destructive test/reset of unrelated data.
- Docs, diagrams, configuration/library records and the requirement coverage matrix match
  the final implementation; no required Team C TODO is left behind.
- Astra findings are resolved and the relevant regressions pass.

The shared issue-closure rule in [#41](https://github.com/aryan12singh/study-buddy-matcher/issues/41)
also requires another team's review and a merge to `main`. That is a release gate after the
user authorises publishing the finished change. Agent review is not a substitute for the
repository's cross-team human review. Missing Team A/B product features are identified
separately; they must not be misreported as implemented or tested by Team C.

## 10. Planned files and ownership boundaries

Final class names may change to preserve single responsibility; the intended file areas
below are the review boundary. No broad refactor outside these areas is planned.

| Area | Existing files/packages to change | New files/areas expected |
| --- | --- | --- |
| C buddy/profile | `backend/src/main/java/com/studybuddy/{matchrequest,connection,profile}/` | Controllers, request/context/viewer-state DTOs and narrow validation/eligibility collaborators |
| C groups | `backend/src/main/java/com/studybuddy/studygroup/` | Controllers, own-group/application reads, viewer-state DTOs and locking queries |
| C notifications | `backend/src/main/java/com/studybuddy/notification/` | Controller, filter/resource/event DTOs and revised schema mapping |
| C admin | `backend/src/main/java/com/studybuddy/admin/` | Controller, creation/detail/summary DTOs, permanent-delete cleanup and last-admin protection |
| B foundations, bounded exception | `user/User.java`, `user/UserRepository.java`, student/course repositories and availability reads | `auth/`, `security/`, `common/error/`, a course-listing service/controller, safe seed bootstrap; reuse equivalent B code if available |
| Database/config, shared | `backend/src/main/resources/application.yml` | Typed foundation settings, `db/migrations/`, test-profile configuration and safe SQL/test setup scripts |
| Backend verification | Existing C unit tests and context test | Integration/security/serialization/concurrency tests and independent test fixtures under `backend/src/test/` |
| C frontend | None of the C feature folders exists yet | `features/connections/`, `features/students/`, `features/notifications/`, `features/groups/`, `features/admin/`, each with API types/hooks/screens/tests |
| B frontend foundation, bounded exception | `frontend/src/App.tsx`, shared CSS tokens, existing auth entry points | `shared/api/`, `shared/auth/`, `shared/components/` and narrowly scoped state/refetch helpers |
| CI/tooling, shared | `.github/workflows/ci.yml` | Existing-tool SQL/isolated-test/demo setup scripts where needed |
| Docs | `README.md`, `AGENTS.md`, `docs/API_CONTRACT.md`, this plan | Decisions, coverage, testing, database operations, handoff and C diagrams listed in section 8 |

`pom.xml`, `package.json` and their lockfiles are not scheduled for dependency changes.
Any unavoidable build/script adjustment must be explicit, justified and included in the
shared-file handoff; a new dependency needs prior agreement. Ignored secret files remain
ignored and are never added to Git. Do not copy the supplied real credentials into tracked
examples, tests, comments, logs or this plan.
