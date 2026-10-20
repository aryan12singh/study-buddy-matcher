# Requirement coverage

Where the compulsory Team A, B and C requirements and merged E2 extension are implemented. This is a source and
acceptance map, not a professor, deployment or submission sign-off. Team A issues are
[#1](https://github.com/aryan12singh/study-buddy-matcher/issues/1),
[#2](https://github.com/aryan12singh/study-buddy-matcher/issues/2),
[#3](https://github.com/aryan12singh/study-buddy-matcher/issues/3) and
[#4](https://github.com/aryan12singh/study-buddy-matcher/issues/4). Team B issues are
[#5](https://github.com/aryan12singh/study-buddy-matcher/issues/5),
[#6](https://github.com/aryan12singh/study-buddy-matcher/issues/6),
[#7](https://github.com/aryan12singh/study-buddy-matcher/issues/7),
[#8](https://github.com/aryan12singh/study-buddy-matcher/issues/8) and
[#9](https://github.com/aryan12singh/study-buddy-matcher/issues/9). Team C issues:
[#10](https://github.com/aryan12singh/study-buddy-matcher/issues/10),
[#11](https://github.com/aryan12singh/study-buddy-matcher/issues/11),
[#12](https://github.com/aryan12singh/study-buddy-matcher/issues/12),
[#13](https://github.com/aryan12singh/study-buddy-matcher/issues/13) and
[#14](https://github.com/aryan12singh/study-buddy-matcher/issues/14).

Backend classes are under `backend/src/main/java/com/studybuddy/`, screens under
`frontend/src/features/`, and HTTP tests under `backend/src/test/java/com/studybuddy/integration/`.
API paths are listed in [API_CONTRACT.md](API_CONTRACT.md).

The compulsory baseline covers #1–14 across all three teams. The dated baseline closure
pass started from `fc1f6fb0526726d06c07a8bbf780b5a61c07399c`, corrected authentication/profile
recovery and implemented the accepted contact contract; its evidence is preserved in
[BASELINE_VERIFICATION.md](BASELINE_VERIFICATION.md). [Team A](TEAM_A_TESTING.md),
[Team C](TEAM_C_TESTING.md) and [E2](E2_TESTING.md) records describe their named dates and
source versions. Source coverage, passing tests and issue closure do not assert grading
acceptance, a member's understanding or actual submission.

## Scope and evidence status

The following implementation snapshot was checked against revision `9592fd2` on
18 October 2026. Later feature records, including E1's own contract and tests, supplement
this baseline/E2 map without changing the dates of historical evidence.

| Category | Authority and scope at the checked revision |
| --- | --- |
| Compulsory baseline | Formal brief: accounts/profiles, weekly availability, matching, requests/contact privacy, groups and admin management; implemented in the source map below |
| Accepted team interpretations | Contact normalization, categorical group-size intervals, learning style through the defined study preferences, and separate deactivation/deletion; see the [baseline contract](DESIGN_DECISIONS.md#baseline-requirement-contract). These do not assert professor confirmation |
| Selected extensions | Revision `9592fd2` includes E2 and optional campus weather. E1 is tracked separately on [PR #61](https://github.com/aryan12singh/study-buddy-matcher/pull/61) and has its own implementation/evidence record |
| Recorded rollout | E2 schema applied on 16 October; see [database operations](DATABASE_OPERATIONS.md#e2-rollout--16-october-2026). A database rollout is distinct from a persistent hosted application |
| Human acceptance | Outlook import remains owner-deferred; cross-team source walkthrough, latest Figma, physical-device/projector checks, rehearsal, LMS deadline and submission receipt require their own evidence |
| Unselected ideas | Public rooms/chat, badges, shared notes/task boards, global online status and AI roadmap/helper proposals are outside the agreed implementation |

## Platform and repeatable setup (#5)

| Requirement / acceptance | Implementation | Automated / runtime evidence |
| --- | --- | --- |
| Java 21/Spring Boot and React/Vite applications with fixed dependencies | `StudyBuddyApplication`, Maven wrapper, frontend entry/router, existing manifests/lockfile | `StudyBuddyApplicationTests`; frontend lint/build; CI |
| Layered controllers/services/repositories with DTO assemblers | Feature packages; `AccountAssembler`, `MyProfileAssembler`, `CourseAssembler`, privacy/group/admin assemblers | Context/HTTP tests and reviewed source boundaries; platform diagrams |
| Shared entities, enums, repositories and persistent PostgreSQL schema | `User`, `Student`, `Course`, `AvailabilitySlot`, preference enums; ordered SQL migrations | `DatabaseBoundaryTest`; fresh/repeated/legacy migration harness |
| Minimum 10 courses and 50 synthetic students; repeat startup preserves existing data | Opt-in `DemoSeedService` reading `demo-seed/demo-data.json` (14 courses, 50 students across six schools); `DemoStudentSeeder` and `DemoScenarioSeeder` use the real profile, request, group and admin services | `DemoSeedTest`: counts, every mode/goal/group size, no-timetable students, Appendix A pair at 0.85 STRONG, presenter demo state, repeat run unchanged; `DemoDataTest` |
| Complete development setup separated from isolated test provisioning | README development package, migrated datasource, `scripts/test-backend.sh` | Literal setup/package and full harness checks recorded with the final branch evidence |
| Backend-only database access and environment-supplied secrets | RLS/revoked browser grants; private ignored config; shared HTTP client | `DatabaseBoundaryTest`; database operations guide |

## Authentication and roles (#6)

| Requirement / acceptance | Backend | Frontend / evidence |
| --- | --- | --- |
| Student registration and login; normalized unique email and encoded passwords | `AuthService`, `AccountCreation`, `InputRules`, `AccountAssembler`, database email index | Auth screens; `Auth.test.tsx`, `AccountCreationTest`, `AccountHttpTest` |
| Public registration cannot select administrator role | Student-only registration contract | `AccountHttpTest`; admin creation is a separate guarded endpoint |
| JWT validation, role guards, inactive/deleted/revoked account rejection | `JwtTokenService`, `JwtAuthenticationFilter`, `SecurityConfiguration`, `AccountAccess` | Auth context/route guards; `JwtTokenServiceTest`, `AccountHttpTest` |
| Successful sign-in updates usage without lost concurrent account edits | Locked current identity in `AuthService` | `LoginConcurrencyTest`, `AccountHttpTest`; failed login preserves usage |
| UTF-8 password byte bound before BCrypt login comparison; generic rejection and unchanged usage | `AuthService` and existing password limit | `AccountHttpTest` boundary, identity-neutrality and legacy-password regressions |
| Invalid/duplicate credentials and safe loading/error feedback | Shared RFC 9457 problem boundary | Auth interaction tests and HTTP rejection cases |

## Own profile and study preferences (#7)

| Requirement / acceptance | Backend | Frontend / evidence |
| --- | --- | --- |
| Read/update own identity/profile fields with required values and field errors | `ProfileService`, `UpdateProfileRequest`, `MyProfileAssembler`, `ProfileProperties` | `EditProfilePage`, `ProfileEditor`, `AboutYouFields`; `ProfileServiceTest`, `EditProfilePage.test.tsx` |
| Course catalogue/enrolments, optional independent buddy target, mode, goals and categorical group size | `CourseService`, `Student`, `GroupSizePreference`; numeric min/max storage | `StudyPreferencesFields`; profile/group-preference tests; API contract |
| Course count/existence, mode/group-size and year limits | Typed profile config and service validation | `ProfileServiceTest`; form validation; empty/invalid states |
| Whole-profile/course/goal replacements use the same student write boundary as weekly replacements | `ProfileService` and account locking | `ProfileConcurrencyTest`; final run in `TEAM_C_TESTING.md` |
| Own contact is editable; another student's contact still follows the privacy assembler | `MyProfileDto` for self; public/connected DTO boundary for other profiles | Profile/HTTP privacy tests; #11 below |
| Accepted Singapore/international contact validation and canonical storage on every write | `InputRules.contactNumber`, account creation, profile and optional admin replacement | Shared frontend contact helper; `ContactHttpTest`, input/service/UI tests; administrator omission/read privacy retained |
| Independent profile/week drafts survive a failed refresh after saving the other section | Owner-keyed profile loader and retained `ProfileEditor` | `EditProfilePage.test.tsx` network/server/course recovery; permission/purge/owner-change clearing |
| Learning style uses the defined study preferences | Team interpretation recorded in `DESIGN_DECISIONS.md`; no separate taxonomy or sixth scorer | Agreed baseline contract; does not claim professor confirmation |

## Weekly availability (#8)

| Requirement / acceptance | Backend | Frontend / evidence |
| --- | --- | --- |
| Replace/clear a student's whole week; whole-minute, same-day start before end | `ProfileService.replaceAvailability`, `AvailabilityRules`, `AvailabilitySlot` | `WeeklySlotEditor`, `WeeklySchedule`; `AvailabilityRulesTest`, `ProfileServiceTest` |
| Own student schedule rejects same-day overlap; group schedules may overlap | `AvailabilityRules` versus `StudyGroup` validation | Caller-specific editor validation; API/design contract distinction |
| Concurrent replacements never commit a mixture of individually valid weeks | Account-row serialization for that student before replacement/read | `ProfileConcurrencyTest`; compulsory integration record |
| Invalid/incomplete raw time text blocks save and triggers exit protection | Shared editor plus `weeklySlotDrafts`; drafts belong to profile/group forms | `WeeklySlotEditor.test.tsx`, `EditProfilePage.test.tsx`, `GroupFormDrafts.test.tsx`, `GroupFormNavigation.test.tsx` |
| Recurring Asia/Singapore day/time survives reload; preview shows saved valid values | Slot DTO/assembler order; no browser-timezone conversion | Profile/editor tests and manual reload/clear checks |

## Shared shell and Home (#9)

| Requirement / acceptance | Implementation | Evidence |
| --- | --- | --- |
| Guarded navigation and reusable retro windows/forms/dialog/states | `AppShell`, `WindowPage`, shared primitives, `routes.tsx` | Auth/dialog/component tests; adopted choices in AGENTS |
| Home uses actual profile, activity, request, group, notification and matching responses | `HomePage`, section-specific API reads, Team A matching preview | `HomePage.test.tsx`, activity/profile/next-step helper tests; real backend audit |
| Active-connections and outgoing-request links select the correct view | Explicit `/connections?view=connected` and `/connections?view=outgoing` destinations | Home/profile rendered navigation regressions |
| Saved target-course hint describes Home preview; current finder intent remains explicit | `StudyPreferencesFields`, Home preview and matching form | Source-aligned copy and matching/profile tests |
| Empty/loading/error states, keyboard focus, wrapping narrow layouts | Shared `StatePanel`, labelled controls and `desktop.css` | Dated Chromium/keyboard checks; 390 px Home smoke check; complete Figma/projector/browser acceptance remains separate |

## Matching algorithm and scoring (#1)

[Issue #1](https://github.com/aryan12singh/study-buddy-matcher/issues/1) uses the formulas and
choices recorded in [DESIGN_DECISIONS.md](DESIGN_DECISIONS.md#matching-decisions-team-a).
This map identifies implementation and regression locations; passing counts and hosted
checks belong in the PR's verified evidence, not a permanently fixed count here.

| Requirement / acceptance | Backend | Screen / documentation | Automated evidence |
| --- | --- | --- | --- |
| Five modular 0-1 scorers with configurable relative importance | `CriterionScorer`, `CourseScorer`, `AvailabilityOverlapScorer`, `StudyModeScorer`, `StudyGoalScorer`, `GroupSizeScorer`, `WeightedMatchScorer` | Matching bars; README formulas | `CriterionScorersTest`, `WeightedMatchScorerTest` |
| Hand-calculated fixtures: full, partial, incompatible and missing data; bounded total/breakdown | Immutable `MatchScore`, `MatchingWeights`, `QualityThresholds` | Compatibility percentage/quality; no probability claim | `MatchingFixtures`, `CriterionScorersTest`, `MatchScoreTest`, `MatchingWeightsTest`, `QualityThresholdsTest` |
| Balanced / availability-first / course-first priorities and disabled priority fallback | `MatchingStrategy` implementations; enabled set applied to scoring/ranking | Active strategy named; backend order retained across qualities | `MatchingStrategyRankingTest`, `PriorityTiersTest`, `MatchServiceTest`, `AppendixAMatchingTest`, `FindBuddiesPage.test.tsx` |
| One-to-one / small-group / either and consistent Either compatibility | Numeric range `GroupSizeScorer`, `MatchSearchFilter`; `StudyModeScorer` | Group/mode filter controls | `CriterionScorersTest`, `MatchSearchFilterTest` |
| Unique weekly overlap; full/partial/none/touching/different-day/duplicate/overlapping inputs | Immutable `TimeSlot`, canonical `WeeklyAvailability` | Shared-hours label and overlap heatmap | `TimeSlotTest`, `MatchServiceTest` |
| Score value includes total, criterion breakdown, shared time and quality; assembler adds deterministic API explanation | `MatchScore`, `MatchResultAssembler`, `MatchExplainer` | Main `MatchCard` shows score/quality, `ScoreBreakdown` and `SharedHoursHeatmap`; explanation text is not rendered | `MatchScoreTest`, `MatchingHttpTest`, `FindBuddiesPage.test.tsx` |
| Active configuration changes rankings without scorer edits; disabled criteria contribute nothing | `MatchingConfigService`, `WeightedMatchScorer`, strategies | Admin enabled criteria and weights; disabled bars identified | `MatchingConfigServiceTest`, `MatchingStrategyRankingTest`, `MatchingHttpTest` |

## Find matches and matching API (#2)

| Requirement / acceptance | Backend | Consumer | Automated evidence |
| --- | --- | --- | --- |
| Course-only, goal-only and combined search; require course or goal; current intent separate from saved target/enrolments | `MatchSearchCriteria`, `MatchService`, `MatchSearchFilter`, focus-aware course/goal scorers | `FindBuddiesPanel`, Home matching preview | `MatchSearchFilterTest`, `CriterionScorersTest`, `MatchingHttpTest` |
| Course, mode, group preference, goal, availability and minimum-quality filters; personal filters cannot mutate settings | `MatchSearchFilter`, quality/threshold filtering in `MatchService` | `MatchFilters` and form/query helpers | `MatchSearchFilterTest`, `MatchServiceTest`, `MatchingHttpTest`, `FindBuddiesPage.test.tsx` |
| Exclude self, inactive/deleted and active connections; retain pending status | `MatchService`, `MatchRelationships` | Pending actions disabled; Connections keeps existing buddies | `MatchServiceTest`, `MatchingHttpTest`, `FindBuddiesPage.test.tsx` |
| Best match / most shared hours / same year first, stable name/ID ties and configured bounded results | Search sort and limit in `MatchService`; typed startup limits | Sort selector, pre-limit count/limited results | `MatchServiceTest`, `MatchingPropertiesTest`, `MatchingHttpTest`, `FindBuddiesPage.test.tsx` |
| Privacy-safe DTO contains display profile, exact score/quality/breakdown, explanation, relevant course, shared time and request state | `MatchResultDto`, `MatchSearchResultDto`, `MatchResultAssembler` | Both matching consumers use `features/matching/api.ts` | `MatchingHttpTest`, `FindBuddiesPage.test.tsx`, `HomePage.test.tsx` |
| Direct API: eligibility, invalid input, auth/roles, no-result result and absence of private fields | `MatchController` delegates to `MatchService`; existing security boundary | Loading/error/empty states | `MatchingHttpTest`, `FindBuddiesPage.test.tsx` |
| Dashboard and matching page share response and active configuration/version | `GET /api/matches` only; no parallel dashboard ranking engine | Home matching preview and Find buddies | `MatchingHttpTest`, `HomePage.test.tsx`, `FindBuddiesPage.test.tsx` |

## Matching page (#3)

| Requirement / acceptance | Implementation | Automated evidence / demo |
| --- | --- | --- |
| Live course-or-goal results; no AI or fabricated fallback matches | `FindBuddiesPage`, `FindBuddiesPanel`, matching API module | `FindBuddiesPage.test.tsx`; test/demo guide scenario 1 |
| Name/programme/year/relevant course/goals, quality and total compatibility; deterministic explanation remains in the API only | `MatchCard`, `MatchResultDto` | Matching rendered tests; scenario 1 |
| Criterion bars, shared totals and overlap heatmap agree with backend | `ScoreBreakdown`, `SharedHoursHeatmap`, immutable score/assembler | Frontend score/presentation tests, `MatchingHttpTest`; scenario 1 |
| All filter types, removable selections/reset and all three sort controls | `MatchFilters`, `matchSearchForm` | `FindBuddiesPage.test.tsx`; scenario 2 |
| Public profile navigation and optional-message request reuse Team C boundaries | `MatchCard`, `SendRequestDialog`, existing profile/request endpoints | Matching frontend tests and `BuddyHttpTest`; scenario 3 |
| Pending/failed request states; no repeat submission; late refresh cannot undo send acknowledgement | `FindBuddiesPanel` request overlay and post-send refresh sequence | `FindBuddiesPage.test.tsx`, including overlapping sends and pre-send replies |
| First-use, incomplete-profile, loading, empty, error/retry states | Matching panel and Home preview with existing `StatePanel`/retro components | `FindBuddiesPage.test.tsx`, `HomePage.test.tsx`; scenarios 1, 2 and 5 |
| Readable and keyboard usable in existing retro shell | Shared primitives, labelled fields/buttons, existing screen layout | Rendered tests and completed local keyboard/layout checks; physical projector remains scenario 5 |
| Full student workflow against live APIs | Search -> filters/sort -> public profile -> optional request -> pending | PostgreSQL HTTP integration, rendered UI tests and completed local Chromium workflow; dated evidence in test guide |

## Admin matching configuration (#4)

| Requirement / acceptance | Backend | Screen | Automated evidence |
| --- | --- | --- | --- |
| Three strategies; enable/disable all five criteria; edit weights and minimum total threshold | `MatchingConfigService`, typed `MatchingProperties`, configuration DTOs/entities | `MatchingSettingsPage`, form helpers | `MatchingConfigServiceTest`, `MatchingPropertiesTest`, `MatchingSettingsPage.test.tsx`, form helper tests |
| At least one enabled criterion, enabled weights total 100%, disabled weights 0; reject negative/zero-total/missing/invalid strategy/threshold | Configuration validation; `MatchingWeights` | Inline validation on save; invalid payloads are not submitted | Backend config/weights tests, `MatchingHttpTest`, frontend form/page tests |
| External defaults, persisted overrides, subsequent-search application, explicit restore, invalid saves preserve last valid state | Persistent settings and override rows; transactional save/restore | Saved/unsaved state, restore confirmation, returned state after save/reload | `MatchingConfigServiceTest`, `MatchingHttpTest`, frontend page tests |
| Admin-only reads/writes/restore; students and unauthenticated callers rejected | `AccountAccess`, security chain, `AdminMatchingConfigController` | Admin route guard; no student config call | `MatchingHttpTest`, frontend guard/page tests |
| Monotonic persistent configuration revision, expected-version conflicts, coherent concurrent search | Versioned settings; first-save serialization; repeatable-read search | Stale-save conflict with reload | `MatchingHttpTest`, config unit tests, frontend config page tests |
| Fractional weights retain precision when saving, editing, reading and scoring | Exact valid weights; legacy-only invalid-total normalization | Precision-preserving form values | Config/property/HTTP tests, `matchingSettingsForm.test.ts`, `MatchingSettingsPage.test.tsx` |
| Overrides survive restart and restore follows current defaults | Database-backed settings read by newly created config service | Reloaded saved state; defaults indicator | PostgreSQL integration plus actual Java-process restart; dated evidence and scenario 4 in test guide |

## Matching completion and human gates

Matching coverage concerns issues #1-#4, implemented in merged PR #54. The original PR and
dated test guide carry their verification results; later merged-main work has its own record.
A source/test map does not assert professor acceptance or that every team member can explain
the implementation.

Local Chromium workflows and keyboard/responsive checks are completed in the
[dated verification record](TEAM_A_TESTING.md#verification-record---13-october-2026).
Follow AGENTS.md's human cross-team review and author walkthrough for subsequent changes. The
physical-projector demo remains a presentation check under the team's actual setup.
The team has recorded its learning-style interpretation under #41/#7; see the
[baseline contract](DESIGN_DECISIONS.md#baseline-requirement-contract). No professor response
or additional taxonomy is asserted.
Team A's class, sequence and use-case diagrams are in [diagrams/TEAM_A.md](diagrams/TEAM_A.md),
and reproducible tests plus manual acceptance steps are in [TEAM_A_TESTING.md](TEAM_A_TESTING.md).
E1 wording (#15), E2 (#16), slides and peer evaluation remain their separate tracked scope.

## Match requests and connections (#10)

| Requirement | Backend | Screen | Tests |
| --- | --- | --- | --- |
| Send a request with an optional message and course/goal context | `MatchRequestService`, `MatchRequestContext` | `students/StudentProfilePage`, `connections/SendRequestDialog` | `BuddyHttpTest`, `MatchRequestServiceTest` |
| Only the recipient can accept or decline a pending request | `MatchRequestService`, `MatchRequest` state transitions | `connections/ConnectionsPage` (Incoming) | `BuddyHttpTest`, `MatchRequestTest` |
| Reject self, duplicate, reverse-duplicate and already-connected requests | `MatchRequestService`, partial unique index | Inline error on send | `BuddyHttpTest`, `MatchRequestServiceTest` |
| Accepting creates exactly one connection; declining creates none | `MatchRequestService`, `Connection` | `connections/ConnectionsPage` | `BuddyHttpTest` |
| Either student can end a connection; history is kept | `ConnectionService` | Disconnect on profile and connections list | `BuddyHttpTest`, `ConnectionServiceTest` |
| Empty, loading and error states | — | `shared/components/StatePanel` | Connections frontend tests |
| Direct outgoing request action and event links open the correct view | Direction-aware notification DTO; explicit outgoing query | Profile request link; Incoming/Outgoing/Connected tabs | `StudentProfilePage.test.tsx`, `NotificationDirectionTest`, navigation tests |

## Profiles and contact privacy (#11)

| Requirement | Backend | Screen | Tests |
| --- | --- | --- | --- |
| Contact number visible only to self and connected students | `ProfileViewAssembler` choosing `PublicProfileDto` / `ConnectedProfileDto` | `students/StudentProfilePage` | `ProfileViewAssemblerTest`, `BuddyHttpTest` |
| Group membership or leadership does not reveal contact | `StudyGroupAssembler`, `GroupViewerAssembler` | `groups/GroupDetailPage` member links | `GroupHttpTest` |
| Disconnecting removes access to contact immediately | `ProfileViewService` (fresh read per request) | Profile refetch after disconnect | `BuddyHttpTest`, profile frontend tests |
| Missing, inactive or unauthenticated profile requests fail safely | `ProfileViewService`, `AccountAccess` | Profile error state | `BuddyHttpTest`, `AccountHttpTest` |
| Browser cannot read tables directly through Supabase | RLS and grants migration | Frontend calls the backend only | `DatabaseBoundaryTest` |

## Study groups and membership (#12)

| Requirement | Backend | Screen | Tests |
| --- | --- | --- | --- |
| Create a course-specific group with goals, mode, capacity and weekly schedule | `StudyGroupService`, `StudyGroup`, `GroupAvailabilitySlot` | `groups/GroupFormPage`, `shared/components/WeeklySlotEditor` | `GroupHttpTest`, `StudyGroupServiceTest` |
| Browse and filter groups; view own groups and applications | `StudyGroupService`, `StudyGroupFilter` | `groups/GroupsPage`, `groups/GroupCard` | `GroupHttpTest`, `StudyGroupFilterTest` |
| View group details and agenda | `StudyGroupAssembler` | `groups/GroupDetailPage` | `GroupHttpTest` |
| Apply to join; leader approves or rejects | `GroupJoinRequestService`, `GroupJoinRequest` | `groups/ApplyGroupDialog`, `groups/GroupManagePage` | `GroupHttpTest`, `GroupJoinRequestServiceTest` |
| Leader counts as a member; capacity cannot be exceeded | `StudyGroup`, `GroupMembership`, group row lock | `shared/components/CapacityMeter` | `GroupHttpTest`, `StudyGroupTest` |
| Leader edits the group and removes members | `StudyGroupService` | `groups/GroupFormPage`, `groups/GroupManagePage` | `GroupHttpTest` |
| Leader closes the group; pending applications are rejected | `GroupClosure` | `groups/GroupManagePage` | `GroupHttpTest` |
| Invalid/incomplete time drafts cannot silently save a previous valid value; overlap remains permitted for group schedules | Same group slot DTO; backend validates complete valid times | Shared editor draft state participates in form save/exit validation | `WeeklySlotEditor.test.tsx`, `GroupFormDrafts.test.tsx`, `GroupFormNavigation.test.tsx` |
| Empty/loading/error/full/closed/non-leader states are handled | Fresh group/viewer state and service authority checks | Groups, detail/manage and application dialog states | `GroupHttpTest`, group browsing/form/dialog tests |

## Notifications (#13)

| Requirement | Backend | Screen | Tests |
| --- | --- | --- | --- |
| Notify on request received, accepted, declined, connection ended and group events | `NotificationService`, called inside each domain transaction | `notifications/NotificationsPage` | `NotificationServiceTest`, `BuddyHttpTest`, `GroupHttpTest` |
| List, filter, mark one read, mark all read, unread count | `NotificationService`, `NotificationFilter` | `notifications/NotificationsPage`, shell badge in `AppShell` | `BuddyHttpTest` |
| Students only see and update their own notifications | `NotificationService`, `AccountAccess` | — | `BuddyHttpTest` |
| Links open the right request tab for the recipient | `NotificationRequestDirection` | `notifications/NotificationsPage` | `NotificationDirectionTest` |
| A failed notification rolls back the change that caused it | Single transaction | Error state with retry | `BuddyHttpTest`, `GroupHttpTest` |

## Admin accounts (#14)

| Requirement | Backend | Screen | Tests |
| --- | --- | --- | --- |
| Admin-only list, search, filter and detail | `AdminUserService`, `AdminUserFilter` | `admin/AdminUsersPage`, `admin/AdminUserDetailPage` | `AccountHttpTest`, `AdminUserFilterTest` |
| Create and edit students and admins | `AdminUserService`, `AccountCreation` | `admin/AdminUserFormPage` | `AccountHttpTest`, `AccountCreationTest` |
| Contact number is write-only for admins | `AdminUserAssembler` | Optional replacement field | `AccountHttpTest` |
| Basic usage: last sign-in, active connections, open group memberships | `UserUsageCounter` | `admin/AdminUserDetailPage` | `AccountHttpTest`, `AdminUserServiceTest` |
| Deactivate and reactivate an account; pending requests become `CANCELLED` | `StudentDeactivation` | `admin/DeactivateUserDialog` | `AccountHttpTest`, `GroupHttpTest`, `NotificationDirectionTest` |
| Permanently delete an account and its dependent records | `StudentDeletion`, `GroupClosure` | Delete confirmation on `admin/AdminUserDetailPage` | `AccountHttpTest` |
| Admins cannot remove themselves, so an active admin always remains | `AdminUserService` | Self controls disabled | `AccountHttpTest`, `AdminUserServiceTest` |
| Deactivated or deleted accounts lose API access | `JwtTokenService`, account version check | Sign-out on 401 | `AccountHttpTest`, `JwtTokenServiceTest` |

## Compulsory integrated acceptance map

Use the current issue matrix, repeatable scenarios and results in
[BASELINE_VERIFICATION.md](BASELINE_VERIFICATION.md). Older detailed Team C scenarios remain
in [TEAM_C_TESTING.md](TEAM_C_TESTING.md). Record the tested SHA and database target when
accepting the compulsory application.

| Behavior to demonstrate | Source / automated coverage | Acceptance evidence to retain |
| --- | --- | --- |
| Register/login, edit own profile/preferences, replace and clear weekly availability | Auth/profile/availability services; unit, rendered and HTTP tests | Valid/invalid saves, reload, invalid-draft exit warning and concurrent complete-week result |
| Search current course/goal, compare stable scores/shared time, change admin strategy/settings and reload | Team A map and `TEAM_A_TESTING.md` | Actual ranked DTO/UI result; settings revision/precision and persistence after restart |
| Send contextual request, follow incoming notification, accept/decline and prevent duplicate decisions | Buddy/request/notification services and `BuddyHttpTest` | Two student identities; pending state, exactly one accepted connection, correct request tab |
| Reveal contact only after acceptance; remove it after disconnect/deactivation | Privacy DTO assembler and HTTP tests | Network payload before/after each state, including stranger and group-only viewer |
| Create/edit/browse/apply/approve/reject/remove/close a group without exceeding capacity | Group row lock/domain rules and `GroupHttpTest` | Leader and applicant roles, pending/full/closed states, complete valid schedule and group overlap policy |
| List/read own notifications, follow correct destination and roll back failed event writes | Recipient scope/direction and transactional notifications | Recipient-only visibility, read/unread badge and rollback regression |
| Admin create/edit/usage/deactivate/reactivate/delete; reject student authority and self removal | Admin/account lifecycle services and `AccountHttpTest` | Revoked JWT/login behavior, ended relations retained on reactivate, dependency cleanup and surviving admin |
| Re-seed without duplicates or resets; install from documented commands | Migration/seed harness and README | 10 courses/50 student minimum, preserved passwords/profile/group data, clean setup result |

Technical source coverage, local automated results, hosted CI, shared-database rollout and
human acceptance must each be recorded separately. Reviewers still need the author walkthrough,
latest-Figma/design check, physical-projector check and integrated demo rehearsal. The
accepted contact/learning-style contract is recorded; it does not assert professor approval.
The coverage file does not itself close or alter GitHub issues.

## Collaborative study experience (E2 #16)

PR [#52](https://github.com/aryan12singh/study-buddy-matcher/pull/52) merged the full
selected E2 feature, including the expressly approved weather enhancement. Its schema
rollout is recorded on [16 October](DATABASE_OPERATIONS.md#e2-rollout--16-october-2026).
Source coverage below is separate from the dated verification record in
[E2_TESTING.md](E2_TESTING.md), actual client imports and persistent application hosting.

| Requirement / acceptance | Backend | Frontend / evidence boundary |
| --- | --- | --- |
| Private group room, accepted active members, join/leave and participant focus/break state | `StudyRoomService`, `RoomAccess`, expiring `RoomPresence` | `StudyRoomPage`, `useStudyRoom`; room HTTP/browser walkthrough |
| Last-seat concurrency, multiple tabs count once, expiry requires explicit fresh admission | Group locking, presence query and capacity checks | `StudyRoomHttpTest`; two-browser expiry/capacity checks |
| Removed/readmitted members cannot revive a lease/role; closure/deactivation cleanup | Transactional `RoomLifecycle`, group and account collaborators | Removal/replacement/readmission regression and access-loss UI/audio stop |
| Leader role assignment; present host/co-host controls and configuration; leader recovery | `StudyRoomService`, observed version and role-preservation checks | `RoomSettingsForm`, timer controls; unauthorized/stale/invalid-setting cases |
| Authoritative shared Pomodoro with focus/break phases and start/pause/resume/reset | Immutable `PomodoroTimer`, durable timestamp state | `RoomTimerPanel`, timer helper; pure timer tests and measured two-browser tolerance |
| Independent shared music/ambient selections and play state; local consent/volume/mute | Layered `RoomAudioDto` and validated presets | Original `RoomAudioEngine`, `RoomAudioPanel`; silent failure and exit/revocation checks |
| Group and connected-buddy dated series with title/timezone/optional agenda/location/link | `StudySessionService`, `SessionAccess`, `SessionDetails`, `StudySession` | `SessionsPage`, `SessionFormPage`, `SessionDetailPage`, `Sessions.test.tsx` |
| Bounded weekly recurrence, correct wall-time/UTC mapping, invalid/DST endpoint rejection | Immutable `SessionSchedule`, typed `SessionSettings` | Schedule unit tests; frontend field validation and raw API rejection |
| Group leader/buddy-participant management, current relationship authorization and stale-write rejection | Parent/session row locking, DTO assembler and version checks | `StudySessionHttpTest`; invalid/stranger/admin/member/ended-parent checks |
| Actual mutual availability without invented attendance or new scoring rules | `SessionAvailability` reuses Team A `WeeklyAvailability.sharedSlotsWith` | Form suggestions and empty-state/manual-time workflow |
| Session update/cancel/lifecycle events reach recipients and cannot leave partial writes | `SessionEvents`, `StudySessionLifecycle`, current recipient rules | HTTP transaction/lifecycle regressions; actual notification navigation |
| Real dated data in Home and group/buddy agendas | Authorized mine/parent-session endpoints | `UpcomingSessions`, `GroupSessionAgenda`, connection entry links; browser save/edit/cancel walkthrough |
| Accurate current/next session labels and safe direct room navigation | Existing authorized occurrence list and validated links; no new endpoint | One view clock follows start/end boundaries, weekly advancement and tab recovery; cancelled/completed plans have no shortcut |
| Duration-aware suggestions with manual fallback | Existing campus-local shared overlap and unchanged save/DST validation | Positive whole-minute choice fills only a fitting overlap; too-short/invalid choices preserve dates and manual scheduling |
| Accessible phase cue and remembered local mixer | Existing authoritative timer and independent shared audio state | Polite phase-only announcement without reconnect replay; safe browser-local volume/mute persistence with explicit playback enable retained |
| Private single/bounded weekly calendar snapshot with stable occurrence IDs and UTC events | `SessionCalendar`, authenticated controller attachment | `CalendarButton`; export unit/HTTP checks, times compared to API occurrences |
| Correct imports and documented update/cancel limitations | Expanded explicit UTC events, escaped/folded RFC 5545 output | Google/Apple single, weekly DST and cancellation imports verified; Outlook acceptance deferred by owner (school MFA), recorded in E2_TESTING |
| Optional campus weather, stale/unavailable fallback and original selectable pixel scenes | `WeatherClient`/`OpenMeteoClient`, bounded `WeatherService` cache and DTO assembler | `RoomWeatherPanel`, theme CSS/helpers, reduced motion; source/CC BY attribution, service/UI tests |
| Backend-only database and documented repeatable rollout | Room/audio/session forward SQL, constraints, RLS and revoked browser grants | `DatabaseBoundaryTest`; ordered migration/adoption checks and reviewed deployment gate |

E2 does not implement E1 AI explanations, public rooms/chat, badge tracking, notes/task
boards, calendar OAuth/two-way sync or an automated reminder scheduler. Exporting a
cancelled event cannot remove a previously imported offline copy, and a revoked
relationship cannot still download the old calendar. Manual weather scenes are
decoration, not forecasts. The recorded database rollout does not establish a persistent
hosted backend/frontend or new calendar-client acceptance.

## Selected extras and delivery

The team has recorded both E1 and E2 as its selected required extras. They extend the
compulsory baseline. The 18 October source snapshot at `9592fd2` contains E2; PR #61
carries E1's separate contract and evidence.

| Tracked scope | Status at the checked revision/date | Acceptance or delivery evidence to complete |
| --- | --- | --- |
| E1 AI match explanation (#15) | Separate [PR #61](https://github.com/aryan12singh/study-buddy-matcher/pull/61); `9592fd2` returns deterministic API text | Use E1's own contract/testing record for subsequent fixes, provider-backed reviewed examples, CI/review/merge and rollout evidence |
| E2 collaborative study room/Pomodoro/music/calendar (#16) | Merged in #52 with weather; schema rollout recorded 16 October | Outlook import acceptance remains owner-deferred. Existing local/browser evidence is dated; team/device/projector acceptance and persistent hosting remain separate |
| README/setup (#17), class/use-case/matching sequence diagrams (#18-20) | Baseline/E2 diagram sources and scalable SVG exports on [#62](https://github.com/aryan12singh/study-buddy-matcher/pull/62); E1 companion on #61 | Final cross-team source review, slide selection and physical-projector legibility; include E1's companion view when presenting that extension |
| Decisions/coverage and library/service/asset inventory (#41/#42) | Baseline choices and source map documented | Final external service/audio inventory, human contribution verification and any later professor-directed contract change |
| AI disclosure (#43) and demo data/integrated acceptance (#44) | Known retained use recorded; repeatable core evidence exists | Every member confirms their retained use; final demo states and deployed/extras acceptance |
| Presentation/slides (#45) and rehearsal/submission (#46) | Unverified in the inspected repository | Editable deck/PDF backup; timed 12-minute demo + 8-minute Q&A; actual hardware/network/audio, verified LMS deadline/receipt and private peer evaluations |

The internal freeze is 24 October; extras/integration delivery targets 25 October, materials
and rehearsal target 8 November, and final handoff targets 15 November 2026 23:59 SGT.
The formal brief states Week 13; verify the actual LMS deadline. Source availability or a
saved/exported document does not prove submission. See the
[schedule record](DESIGN_DECISIONS.md#schedule-and-submission-record).

## AI match explanation (E1, #15)

Implementation on the E1 branch is distinct from cross-team review, merge and live-provider
acceptance. [E1_TESTING.md](E1_TESTING.md) is the current regression and demo guide.

| Requirement / acceptance | Backend | Frontend | Evidence |
| --- | --- | --- | --- |
| Explain a current card without changing scores/ranking | `MatchEvaluationFingerprint`, `MatchService.evaluate`, `MatchExplanationService`; stale 409 before optional work | `evaluationId` request/response check and Refresh matches action | `MatchServiceTest`, `MatchExplanationHttpTest` config/profile/availability drift |
| Clear old answers on real refreshes | Candidate/evaluation ID identifies the result | Keyed panel lifecycle, abort/ignore late replies; unchanged relationship-only updates retain answers | `MatchExplanationPanel.test.tsx`, `FindBuddiesPage.test.tsx` completed/in-flight post-send cases |
| Send only allowlisted anonymous evidence with accurate search intent | `ExplanationEvidenceFactory`, shared `StudyGoalIntent`; scoring/shared-course distinction | AI label and existing score breakdown | Evidence/privacy tests and goal/course regressions in `MatchServiceTest` |
| Apply bounded output checks and state their limits | Prompt v5 with plain-word evidence and weights; evidence-aware course, number and code-name checks; current length policy for fresh/cached text | AI accuracy cue; literal text rendering | `ExplanationOutputValidatorTest`, prompt tests, panel rendering tests; live examples are separate acceptance |
| Fall back on provider/cache failures without hiding core errors | Provider timeout/error handling; quota; separate cache-read/write `DataAccessException` boundaries | Standard explanation and ordinary retry states | Provider loopback, service and quota tests; no live provider required |
| Reuse equivalent evidence without confusing it with card identity | Separate semantic hash; prompt/model versioning; unique cache row | — | `ExplanationHasherTest`, `MatchExplanationHttpTest`; cache-hit policy tests |
