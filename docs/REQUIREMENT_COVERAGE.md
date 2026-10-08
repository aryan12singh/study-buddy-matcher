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
