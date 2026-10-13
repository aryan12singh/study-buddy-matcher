# Requirement coverage

Where the Team A and Team C scoped requirements are implemented. Team A issues are
[#1](https://github.com/aryan12singh/study-buddy-matcher/issues/1),
[#2](https://github.com/aryan12singh/study-buddy-matcher/issues/2),
[#3](https://github.com/aryan12singh/study-buddy-matcher/issues/3) and
[#4](https://github.com/aryan12singh/study-buddy-matcher/issues/4). Team C issues:
[#10](https://github.com/aryan12singh/study-buddy-matcher/issues/10),
[#11](https://github.com/aryan12singh/study-buddy-matcher/issues/11),
[#12](https://github.com/aryan12singh/study-buddy-matcher/issues/12),
[#13](https://github.com/aryan12singh/study-buddy-matcher/issues/13) and
[#14](https://github.com/aryan12singh/study-buddy-matcher/issues/14).

Backend classes are under `backend/src/main/java/com/studybuddy/`, screens under
`frontend/src/features/`, and HTTP tests under `backend/src/test/java/com/studybuddy/integration/`.
API paths are listed in [API_CONTRACT.md](API_CONTRACT.md).

## Matching algorithm and scoring (#1)

[Issue #1](https://github.com/aryan12singh/study-buddy-matcher/issues/1) uses the formulas and
choices recorded in [DESIGN_DECISIONS.md](DESIGN_DECISIONS.md#matching-decisions-team-a).
This map identifies implementation and regression locations; passing counts and hosted
checks belong in the PR's verified evidence, not a permanently fixed count here.

| Requirement / acceptance | Backend | Screen / documentation | Automated evidence |
| --- | --- | --- | --- |
| Five modular 0-1 scorers with configurable relative importance | `CriterionScorer`, `CourseScorer`, `AvailabilityOverlapScorer`, `StudyModeScorer`, `StudyGoalScorer`, `GroupSizeScorer`, `WeightedMatchScorer` | Matching bars; README formulas | `CriterionScorersTest`, `WeightedMatchScorerTest` |
| Hand-calculated fixtures: full, partial, incompatible and missing data; bounded total/breakdown | Immutable `MatchScore`, `MatchingWeights`, `QualityThresholds` | Compatibility percentage/quality; no probability claim | `MatchingFixtures`, `CriterionScorersTest`, `MatchScoreTest`, `MatchingWeightsTest`, `QualityThresholdsTest` |
| Balanced / availability-first / course-first priorities and disabled priority fallback | `MatchingStrategy` implementations; enabled set applied to scoring/ranking | Active strategy named; backend order retained across qualities | `MatchingStrategyRankingTest`, `MatchServiceTest`, `FindBuddiesPage.test.tsx` |
| One-to-one / small-group / either and consistent Either compatibility | Numeric range `GroupSizeScorer`, `MatchSearchFilter`; `StudyModeScorer` | Group/mode filter controls | `CriterionScorersTest`, `MatchSearchFilterTest` |
| Unique weekly overlap; full/partial/none/touching/different-day/duplicate/overlapping inputs | Immutable `TimeSlot`, canonical `WeeklyAvailability` | Shared-hours label and overlap heatmap | `TimeSlotTest`, `MatchServiceTest` |
| Score value includes total, criterion breakdown, shared time, deterministic explanation and quality | `MatchScore`, `MatchResultAssembler` | `MatchCard`, `ScoreBreakdown`, `SharedHoursHeatmap` | `MatchScoreTest`, `MatchingHttpTest`, `FindBuddiesPage.test.tsx` |
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
| Name/programme/year/relevant course/goals, quality, total compatibility and deterministic explanation | `MatchCard`, API DTO | Matching rendered tests; scenario 1 |
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

## Shared completion and human gates

Technical coverage above concerns issues #1-#4 on PR #54. The PR carries the exact local
and hosted verification results for its final SHA. A source/test map does not by itself
assert a merge, professor acceptance or that all team members can explain the implementation.

Local Chromium workflows and keyboard/responsive checks are completed in the
[dated verification record](TEAM_A_TESTING.md#verification-record---13-october-2026).
Before merging, follow AGENTS.md's human cross-team review and author walkthrough. The
physical-projector demo remains a presentation check under the team's actual setup.
The undefined learning-style wording remains a precise professor clarification under #41/#7;
see [DESIGN_DECISIONS.md](DESIGN_DECISIONS.md#undefined-learning-style-wording-417).
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
