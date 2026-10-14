# Compulsory integration and Team C verification

## Scope and current status

This is the historical 13 October verification record. The branch was subsequently merged
as PR #55. Current baseline closure, contact/learning-style choices and later regressions
are recorded in [BASELINE_VERIFICATION.md](BASELINE_VERIFICATION.md).

This record covers the compulsory A/B/C application and the merged-main audit fixes on
`fix/team-c/merged-main-completion`, based on main
`b33f286887a090cd9b765886263c0590a13a6acf`.

Runtime fixes are in `1f380cc` (profile/availability serialization) and `b1769bf`
(time drafts, group form state, navigation/copy and asset cleanup). The documentation commit
adds this record and the updated setup/coverage/rollout material; it does not change runtime
behavior.

Team C's implemented compulsory scope is requests/connections, profile privacy, groups and
membership decisions, notifications and account administration (#10–14). The table below
records technical verification rather than asserting teammate approval or viva acceptance.
E1/E2, the separate room branch #52, slides, rehearsal and submission are outside this pass.

## Repeatable automated checks

Select JDK 21 first. From the repository root:

```bash
scripts/test-backend.sh
```

The runner creates private PostgreSQL 17 databases, applies/repeats migrations and checks
legacy upgrade/preflight rollback before running the actual Spring/JWT/HTTP suite. Its
destructive fixture reset refuses any database other than `studybuddy_test`. Never point
the fixture or migration tests at shared Supabase.

Frontend checks, from `frontend/` after `npm ci`:

```bash
npm test
npm run lint
npm run build
```

For a development package, from `backend/`:

```bash
./mvnw -DskipTests package
```

This last command deliberately skips tests. Private `application-local.yml` configures
the running app; it is not a test-database provisioning mechanism. Follow the README's
database migration, private configuration and startup steps before running the application.

## Team C acceptance map

| Issue / behavior | Automated technical evidence | Repeatable live scenario |
| --- | --- | --- |
| #10 Send, list and answer contextual requests; reject self/duplicate/invalid decisions; end connections | `BuddyHttpTest`, `MatchRequestServiceTest`, `ConnectionServiceTest`, connection UI tests | Search or open another student's profile, send a request, follow its notification as the receiver, accept/decline, and inspect both request histories and connected view |
| #10 Relationship actions stay current and outgoing links open the correct tab | Matching/profile/connection UI tests and `StudentProfilePage.test.tsx` | After sending, “View your requests” opens Outgoing; Home's Active connections opens Connected buddies |
| #11 Contact withheld until an active buddy connection; remove access after end/deactivation; group membership/admin authority does not bypass privacy | `BuddyHttpTest`, `GroupHttpTest`, `AccountHttpTest`, profile assembler/UI tests, `DatabaseBoundaryTest` | Inspect the public DTO before accept, the connected DTO afterward, and the public DTO after access ends. Check an ordinary group member separately |
| #12 Browse/filter/create/edit groups with goals/mode/capacity and weekly times | `GroupHttpTest`, `StudyGroupServiceTest`, group UI/form tests | Create a course group, edit its details/schedule and compare the returned group data |
| #12 Apply/approve/reject, leader authorization, last-place capacity, remove member and close group | `GroupHttpTest`, `GroupJoinRequestServiceTest`, management UI tests | Apply as another student, approve as leader, inspect member count, remove that member, then close the group. Ordinary members cannot manage |
| #12 Invalid/incomplete drafts block group saves; discard/row operations preserve the right edits; creation does not inherit a previous group | `GroupFormDrafts.test.tsx`, `GroupFormNavigation.test.tsx`, `WeeklySlotEditor.test.tsx` | Type `25:00` or clear a time, save, and verify no POST/PUT. Leave and check discard protection. Correct the draft and save; creation starts with an empty form |
| #13 Own notifications, filtering/read/unread count and correct event links | `BuddyHttpTest`, `NotificationDirectionTest`, `NotificationServiceTest`, notification UI tests | Follow a received buddy-request notification, verify read state and Incoming target; repeat with acceptance/decline/group events |
| #14 Admin list/detail/create/edit, write-only contact input and usage | `AccountHttpTest`, `AdminUserServiceTest`, admin UI tests | Create/edit a synthetic student; inspect admin responses for absence of contact and view recorded usage |
| #14 Deactivate/reactivate/permanent delete, dependent cleanup and old-token rejection | `AccountHttpTest`, `GroupHttpTest`, admin UI tests | Deactivate a connected account; old access fails. Reactivate and sign in afresh; relationships stay ended. Delete a separate synthetic account with typed confirmation |
| #14 Self-removal and concurrent account/relationship mutations stay safe | `AccountHttpTest`, account-access/lifecycle unit tests | Verify self controls remain unavailable; concurrent cases are exercised directly through real HTTP/PostgreSQL |

The supported group-management contract is leader decisions/removal and closure. Dated
sessions, rooms, timers, audio and calendar exports belong to E2, not this acceptance table.

## Confirmed merged-main fixes

- Profile and availability replacements lock the student account before reading state.
  Two concurrent submissions retain one complete update rather than combining weeks,
  courses or study goals. Different accounts still write independently.
- Weekly drafts live in their owning forms. Invalid/unfinished drafts prevent saving,
  count as unsaved work and survive profile tabs and unaffected row operations. Valid
  shorthand is finished on blur or submit.
- Student availability rejects overlaps; group schedules retain their documented
  overlap-permitting policy. Neither path silently submits an old time for an invalid draft.
- Discarding group edits and navigating to creation resets details, baseline, errors and
  drafts. Moving to another edit identity initializes the new group's data.
- The Home Connected and outgoing-profile request links select the correct tabs. The
  saved-target hint describes the Home preview/current-search context accurately.
- The README separates development packaging from isolated integration tests, describes
  migration prerequisites and avoids claiming Hibernate supplies every integrity/access rule.
- Adopted components/layout, platform coverage/diagrams, known AI use and asset inventory
  are documented. Unverified SVGs were replaced/removed; no dependency was introduced.

## Verification record — 13 October 2026

### Automated checks

| Check | Result |
| --- | --- |
| Full Java 21 backend and PostgreSQL integrity harness | **367 tests passed; zero failures/errors/skips** |
| Full frontend suite | **254 tests passed across 28 files** |
| Frontend lint, TypeScript and production build | Passed; existing bundle-size advisory remains nonblocking |
| Backend development package | Passed |
| Fresh development copy, without any test database/JWT environment | `./mvnw -DskipTests package` passed |
| Fresh frontend installation and production build | `npm ci` and `npm run build` passed |
| Default local-profile startup in the fresh copy | `./mvnw spring-boot:run` loaded private local YAML and started against an isolated migrated database |
| Independent final code review | No actionable findings in the runtime changes and reviewed documentation |
| Whitespace and documentation links | Checked before the documentation commit |

Before the account-lock fix, both new `ProfileConcurrencyTest` scenarios failed the
original code with combined collections. The fixed run observes the second same-account
request waiting for the first transaction's row lock, while another account can replace
availability. Before the group reset, its new route regression retained the old group's
name in creation. These are reproduced regressions, not assertions that only mirror code.

The original main's hosted CI run
[38403761559](https://github.com/aryan12singh/study-buddy-matcher/actions/runs/38403761559)
passed at `b33f286`. At the end of the local verification recorded here, the branch had not
yet been pushed and no PR was open. That historical run is not a hosted check of these fix
commits. The subsequent PR records its own branch CI and cross-team review results.

### Live application checks

Chromium exercised the real frontend, Java backend and migrated PostgreSQL with synthetic
identities. API responses were inspected alongside the visible outcomes:

- Invalid `25:00` in profile availability caused no PUT, showed validation and opened
  a discard dialog on leaving. Correcting to `0930` saved `09:30:00` with HTTP 200.
- Home's Active connections opened `?view=connected`. After a contextual request,
  the profile's request link opened `?view=outgoing`.
- A goal-based search returned nine real results with the active strategy. Public
  profiles omitted contact; receiving/accepting a request returned 200 and revealed
  contact only through the connected DTO. Following its notification marked it read.
- Invalid times blocked both group creation and editing. Valid times saved as
  `09:30:00` on create (201) and `09:45:00` on edit (200); leaving the invalid edit
  required discard confirmation.
- A student applied (201), the leader approved (200), and capacity reached two of two.
  Ordinary membership did not reveal contact or leader-only pending counts.
- The leader removed a member (204) and closed the group (200); the group became inactive.
- Admin creation (201), editing (200) and typed-confirmation deletion (204) worked.
  Admin responses omitted the private contact input.
- Deactivation blocked the correct-password login with 403. Reactivation allowed a fresh
  login with 200; the former buddy relation remained STRANGER and omitted contact.
- Saved matching configuration survived a real Java restart: COURSE_FIRST, minimum 0.1,
  revision 1. Repeated seeding retained 10 courses, 50 profiles and one admin, with identical
  password hashes, profile and group data.
- The actual shared database passed Java startup with schema validation and seeding disabled
  after the protected rollout. Full schema/backup/history evidence is recorded in
  [DATABASE_OPERATIONS.md](DATABASE_OPERATIONS.md#compulsory-rollout--13-october-2026).

Full-suite HTTP tests supply additional empty/invalid/unauthorised/concurrent cases; the
browser list above describes the specific live interactions actually performed.

## Handoff and remaining human gates

All confirmed compulsory runtime defects from the merged-main audit are patched, with the
two further reproduced profile-collection/group-route defects included. Team C's #10–14
source coverage and technical checks are recorded here. No bonus code or room branch changes
are included, and no GitHub issue state has been changed as a substitute for acceptance.

At this snapshot, publication, branch CI and cross-team review were the next delivery steps.
PR #55 later merged these changes; the baseline closure record carries the subsequent checks
and accepted contact/learning-style contract. Authors must still explain retained changes.
Projector/Figma/browser acceptance, diagram exports, presentation, peer evaluations and final
submission are separate human/delivery gates.
