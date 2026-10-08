# Requirement coverage

Where each Team C requirement is implemented. Issues:
[#10](https://github.com/aryan12singh/study-buddy-matcher/issues/10),
[#11](https://github.com/aryan12singh/study-buddy-matcher/issues/11),
[#12](https://github.com/aryan12singh/study-buddy-matcher/issues/12),
[#13](https://github.com/aryan12singh/study-buddy-matcher/issues/13) and
[#14](https://github.com/aryan12singh/study-buddy-matcher/issues/14).

Backend classes are under `backend/src/main/java/com/studybuddy/`, screens under
`frontend/src/features/`, and HTTP tests under `backend/src/test/java/com/studybuddy/integration/`.
API paths are listed in [API_CONTRACT.md](API_CONTRACT.md).

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

## E2 room foundation (#16, partial)

| Implemented slice | Backend | Screen | Evidence |
| --- | --- | --- | --- |
| Active accepted members only; contacts withheld | `RoomAccess`, `RoomAssembler`, RLS migration | `studyroom/StudyRoomPage`, active-group entry link | `StudyRoomHttpTest`, `DatabaseBoundaryTest` |
| Immutable repeating focus/break timer, pause/resume/reset | `PomodoroTimer`, `StudyRoomService` | `RoomTimerPanel` | 22 `PomodoroTimerTest` cases; HTTP and timer-rendering tests |
| Distinct-student capacity and expiring per-tab presence | Group lock, `RoomPresence`, `RoomPresenceRepository` | Join/leave and participant status | HTTP tests: last-slot race, multi-tab, expiry, actor scoping |
| Host/co-host assignment, current version and recovery | `StudyRoomService`, `RoomAssembler` | Leader settings, role-specific controls | HTTP and frontend tests: non-host denial, stale edits, leader recovery |
| Lifecycle access loss and permanent-delete cleanup | Eligibility queries and FK cascade/set-null | Purge room snapshot and stop audio on failure | HTTP tests: removal/deactivation/closure/deletion; frontend rejected-heartbeat test |
| Original shared audio with local consent/volume/mute | `AudioPreset`, persisted preset/play state | `RoomAudioEngine`, `RoomAudioPanel` | Audio consent, local-only controls and failure frontend tests |

Local verification on 9 October 2026: full backend suite **262 passed**, including
fresh/legacy/repeat migration checks against disposable PostgreSQL 17. Frontend
suite **123 passed**; lint had no warnings and the production build passed.

An actual two-account browser walkthrough used a disposable local database and
confirmed group entry, saved durations/host assignment, two distinct participants,
focus-to-break progression, shared pause/resume, shared rain selection, separate
local audio enable/mute, and refresh/rejoin with retained timer state. Both clocks
displayed `01:00` after start and `00:56` when paused in break, matching to the
displayed second in this local sample. The configured poll is two seconds; this
sample is not a production latency guarantee. Closing the host browser expired
its presence, while the timer continued and the leader could pause it. No browser
console errors/warnings were observed. Automated tests additionally cover rejected
heartbeats, stale edits, capacity races and lifecycle cleanup.

The final source review found a post-leave heartbeat ordering bug. Participation
now changes and pending polls cancel before the shared refresh event. The added
regression and a browser leave/preview check passed; the review confirmed the fix.

The room migration has not been applied to shared Supabase. Cross-team review and
an author walkthrough remain delivery gates. The branch is a complete room
foundation, not a claim that the whole E2 bonus is accepted.

**Deliberately remaining for teammates:** dated group/buddy sessions, session
agenda/editor, calendar exports and real calendar-import acceptance, mutual
availability via Team A, session notifications/dashboard integration, and E1 AI
explanations. Neither #16 nor #15 is completed by this foundation. The next slices
and integration boundaries are in [BONUS_IMPLEMENTATION_PLAN.md](BONUS_IMPLEMENTATION_PLAN.md).
