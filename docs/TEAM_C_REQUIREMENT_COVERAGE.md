# Team C requirement coverage

Maps current issues [#10](https://github.com/aryan12singh/study-buddy-matcher/issues/10),
[#11](https://github.com/aryan12singh/study-buddy-matcher/issues/11),
[#12](https://github.com/aryan12singh/study-buddy-matcher/issues/12),
[#13](https://github.com/aryan12singh/study-buddy-matcher/issues/13) and
[#14](https://github.com/aryan12singh/study-buddy-matcher/issues/14) to source and verified
behavior. Earlier duplicate closed issues #30-34 are not completion evidence for this branch.
Cross-team human review, hosted CI and merge remain the release gate under #41.

All backend paths below have `/api` prefix. HTTP tests run the real security/web/service/JPA
stack against migrated PostgreSQL 17, rather than mocked controllers. Class names are under
`backend/src/test/java/com/studybuddy/`; frontend suites are beside their features/shared code.

## C1: requests and connections

| Acceptance behavior | Backend / API | Screen | Evidence |
| --- | --- | --- | --- |
| Optional message and validated course/search context | MatchRequestService, context DTO; POST match-requests | Profile and reusable SendRequestDialog; incoming/outgoing cards | BuddyHttpTest.requestUsesAuthenticatedSenderAndPreservesMatchingContext; Connections tests; two-account browser send |
| Authenticated sender; recipient-only pending decision | Principal + AccountAccess + locked request; POST id/accept or decline | Incoming actions only, pending disabled during write | BuddyHttpTest contact journey/invalid inputs; service authorization tests |
| Reject self/reverse duplicate/existing buddy/inactive or missing target | Sorted account locks, service state checks, partial pair index | Inline/action error and retry | BuddyHttpTest concurrentOppositeRequests; AccountHttpTest deletion/send race; unit edge cases |
| Exactly one symmetric connection on acceptance; none on decline | Same transaction state+Connection+notification | Connected buddies list and relationship DTO | ConcurrentAcceptance and acceptance/decline race HTTP tests; actual receive/accept browser flow |
| Either participant ends owned active connection; history remains | ConnectionService; DELETE connections/id | Confirmed Disconnect from profile/list | BuddyHttpTest contactIsWithheldUntilAcceptanceAndRemovedImmediatelyOnDisconnect; browser confirmation/reload |
| Lists, dashboard counters and notifications update | incoming/outgoing/connections/student me summary; transactional events | Three tabs, counts/badges, mutation/focus/interval refetch | BuddyHttpTest notifications/counts; shell badge interaction test; browser request history |
| Safe empty/loading/error/resubmission states | Safe problems, locked terminal transitions | Shared StatePanel/Button/useAction | Connections/useResource tests; blank seeded account browser |

## C2: profiles and privacy

| Acceptance behavior | Backend / API | Screen | Evidence |
| --- | --- | --- | --- |
| One backend privacy policy, DTOs only | ProfileViewAssembler; GET students/id/profile | Public and connected profile states | Existing assembler tests + literal HTTP field/value assertions |
| Self and active buddy may read contact; stranger/pending/declined/ended cannot | Relationship assembler selects PublicProfileDto/ConnectedProfileDto | Hidden-contact guidance or supplied contact | BuddyHttpTest full lifecycle, Profile tests and live acceptance/disconnect |
| Group membership/leadership alone grants no contact | Group member/public profile DTOs, admin contact omitted | Profile navigation from group member/leader links | GroupHttpTest peer privacy; AccountHttpTest private admin response assertions |
| Requests/groups/events/admin cannot serialize stored contact/hash/token | DTO assemblers and safe event copy | No client CSS concealment or persisted profile cache | HTTP payload assertions, frontend profile/cache tests, code layering check |
| Invalid/inactive/missing/unauthenticated profile safe | Account eligibility + 404/401, profile no-store | Validated route, loading/error/retry | BuddyHttpTest and deactivation/deletion HTTP cases; profile rendered tests |
| Revoked data refetched/cleared after disconnect/logout | Fresh assembler per request, no-store | Resource purge/abort, token-only session storage | Profile privacy tests, useResource stale response test, live disconnect/reload |
| Alternate Supabase path denied | RLS + grants migration on 16 tables | Frontend uses backend only | DatabaseBoundaryTest SQLSTATE42501 checks; hosted RLS/grants verification |

## C3: study groups and memberships

| Acceptance behavior | Backend / API | Screen | Evidence |
| --- | --- | --- | --- |
| Create course-specific group with all supplied fields | StudyGroupService, availability repository; POST groups | Full create form/weekly slot editor | GroupHttpTest lifecycle and invalid slots; Groups tests; browser saved fields |
| Browse/filter/detail/own groups/applications | GET groups filters, groups/id, groups/mine, group-join-requests/mine | Browse, Your groups, Your applications, group detail | HTTP filters/member state/history, rendered tests, two-account browser |
| Goals/weekly schedule agenda reflects saved backend data | Detail DTO fields | View agenda dialog | Groups agenda test and browser Tuesday09:00-11:00 display |
| Leader counted as accepted member; pending does not consume capacity | Leader membership + count, group row lock | Capacity badges and management counts | GroupHttpTest count/last-place races; group unit tests |
| Separate join request/leader approval/rejection | GroupJoinRequestService; group-scoped request actions | Applicant dialog and leader applicants tab | GroupHttpTest unauthorized/scoped mismatch/duplicate/history; browser approval |
| Reject duplicate/member/inactive/full/closed cases and repeated decisions | Group/account locks and partial applicant index | Pending/member/full/closed UI states | Unit cases, concurrent duplicate and approval HTTP tests |
| Capacity edits and approvals cannot overfill | Same locked group before count/update | Inline capacity checks and safe conflict | GroupHttpTest concurrentApprovals and capacityReductionAndApproval |
| Update all information, remove member, protect leader | PUT groups/id, DELETE groups/id/members/studentId | Prefilled edit form, confirmed removal | HTTP lifecycle, Groups tests, live edit/remove verification |
| Close one-way, reject pending requests, notify accepted others | GroupClosure; POST groups/id/close | Confirmed close, closed record/history | GroupHttpTest close/approve race, UI closed state and browser closure |
| Safe profile links and no peer contact | Public member DTOs/eligible profile lookup | Member links/contact guidance | GroupHttpTest group peer privacy; browser member profile |

## C4: notifications

| Acceptance behavior | Backend / API | Screen | Evidence |
| --- | --- | --- | --- |
| Correct received/accepted/declined/ended/group lifecycle recipients | One NotificationService used inside domain transaction | Linked event cards | NotificationService/domain tests, HTTP exactly-one event assertions |
| Store safe text/type/resource/time/read and event identity | NotificationDto + resource/event columns, Instant | Newest first, SGT Today/Earlier, unread indicators | Service/filter tests; real HTTP time/payload checks; Notifications tests |
| Current recipient list/read-one/read-all/unread count | GET notifications/filter/unread-count, POST read/read-all | All/Requests/Groups tabs, read actions, shell badge | BuddyHttpTest notifications recipient/read persistence; shell integration test |
| Ownership enforced; failed/repeated writes do not duplicate events | AccountAccess + recipient checks + event partial unique index | Action failure states/disable | HTTP403, race and DB dedup tests |
| Domain + notification rollback together | Single transaction | Failure returns safe problem, can retry | BuddyHttpTest and GroupHttpTest injected notification DB failures |
| Deleted/unavailable targets remain safe | Hard-delete purge + generic null-link events | Link routing and missing-resource state | AccountHttpTest deletion resource cleanup, Notifications tests |

## C5: administrator accounts

| Acceptance behavior | Backend / API | Screen | Evidence |
| --- | --- | --- | --- |
| Admin-only list/detail/search/role/status/summary | AdminUserService and controllers | Account list/filters/summary and detail | AccountHttpTest create/edit/filter/non-admin assertions; Admin tests |
| Create student/admin and edit prefilled public fields | AccountCreation/assembler; POST and PUT admin/users | Create/detail/edit forms, immutable role on edit | Atomic invalid-create/UTF8/duplicate HTTP tests and UI interactions |
| Contact input-only, omitted replacement preserved | Account update policy, contact never assembled for admin | Blank optional replacement with clear hint | AccountHttpTest DB value preservation and literal absence; Admin tests |
| Real last login/active buddies/accepted open-group count, no-activity zeros | User.recordLogin + UserUsageCounter | Timestamp/never logged in/zero usage | AccountHttpTest login and lifecycle counters; usage unit tests |
| Separate deactivate/reactivate/permanent delete with clear confirmations | Authorized lifecycle transaction and cleanup collaborators | Distinct actions, delete email confirmation | AccountHttpTest cleanup/old JWT after reactivate; live admin journey |
| Inactive/deleted account cannot use protected API or be a C target | JWT current account/version + service eligibility | Expiry/logout and unavailable profile handling | Malformed/expired/revoked token tests; cleanup/race HTTP tests |
| No orphan active buddy/member/led resources, no broken deleted links | StudentDeactivation / StudentDeletion / GroupClosure | Real refreshed status and counts | Full dependency HTTP fixtures, delete/send and deactivate/approve races |
| Self/last-admin removal protected across concurrent actions | Exclusive guard + locked administrator recount | Self controls disabled, safe conflict | AccountHttpTest selfRemovalAndConcurrentAdministratorRemoval; unit last-admin cases |

## Foundations and delivery evidence

Auth/current principal/role guards/course listing, synthetic seed, shared client/primitives,
schema/access policy, test setup and CI are implemented only to make C operable and
reusable by A/B. The seed is opt-in, has private runtime passwords and is idempotent.
The shared database has the upgraded schema and 50 students/10 courses; a repeat preserved
the identity/password/creation checksum. The live backend passed JDBC schema validation and
authenticated shared-data reads. Browser lifecycle tests use an isolated database, not shared
destructive data. All automated commands and actual outcomes are in [TEAM_C_TESTING.md](TEAM_C_TESTING.md).

Docs include current contracts/configuration/libraries, accepted decisions, class/ER/sequence
diagrams, operations/recovery, demo/integration handoff, and truthful contribution/assistance
records. No compulsory C behavior is represented by a TODO or fabricated mock response.

The one final Astra sweep produced three P2 findings, all corrected and tested: initial locked
login lookup (`LoginConcurrencyTest`, actual row-lock red/green regression), request-dialog
draft retention through focus/polling while purging private data (four rendered cases), and
recipient request direction for withdrawal/answered notification links (`NotificationDirectionTest`,
three HTTP cases plus unit/rendered cases and a live link journey). The approved follow-up
also fixes late-save navigation/admin refresh state and batches list reads, with regression
evidence in TEAM_C_TESTING.md. Current results are **228 backend tests and 77 frontend tests**,
with passing lint/build and migration checks.

Published as [PR #48](https://github.com/aryan12singh/study-buddy-matcher/pull/48); initial hosted
CI passed, and the PR checks show each pushed revision. Remaining external work: another team's
human review, both owners' walkthrough, latest-commit CI verification and merge, A/B product
integration, shared original-asset provenance, team slides/rehearsal and
later extras. These do not imply that matching, the full dashboard or extras were built in C.
