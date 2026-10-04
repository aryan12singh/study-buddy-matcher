# API contract

The agreed surface between the React frontend and the Spring Boot backend.

**Agree the row before either side builds against it.** The most expensive failure available to
a three-team project is Team A's matching page calling an endpoint Team B never agreed to
build, discovered the week of the demo. Filling in a row here costs two minutes; finding out
on 10 October costs an evening.

How to use this file:

1. Whoever needs the endpoint opens a pull request adding the row, with the request and
   response shapes filled in.
2. The owning team reviews and merges it.
3. Both sides build against the merged row. A change after that is another pull request, and
   the other side gets told.

Conventions: base path `/api`, JSON in and out, `Authorization: Bearer <token>` on everything
except registration and login. Responses are DTOs, never entities. Errors use the standard
Spring problem shape. TODO: confirm and document the exact error body once Team B has the
exception handler in place.

---

## Auth and profile (Team B)

| Method | Path | Request | Response | Auth | Status |
| ------ | ---- | ------- | -------- | ---- | ------ |
| POST | `/api/auth/register` | TODO | TODO | Public | TODO |
| POST | `/api/auth/login` | TODO | TODO | Public | TODO |
| GET | `/api/profile/me` | none | TODO | Student | TODO |
| PUT | `/api/profile/me` | TODO | TODO | Student | TODO |
| PUT | `/api/profile/me/availability` | TODO | TODO | Student | TODO |
| GET | `/api/courses` | none | TODO | Student | TODO |

## Matching (Team A)

| Method | Path | Request | Response | Auth | Status |
| ------ | ---- | ------- | -------- | ---- | ------ |
| GET | `/api/matches` | TODO: **a course or a study goal is the entry point and one of the two is required**; filters (course, availability, study mode), sort, strategy and threshold are additional query parameters | TODO: ranked matches, each carrying a `MatchScore` with its per-criterion breakdown | Student | TODO |
| GET | `/api/admin/matching-config` | none | TODO: current weights, threshold, active strategy | Admin | TODO |
| PUT | `/api/admin/matching-config` | TODO | TODO | Admin | TODO |

## Requests and connections (Team C)

| Method | Path | Request | Response | Auth | Status |
| ------ | ---- | ------- | -------- | ---- | ------ |
| POST | `/api/match-requests` | `{ "receiverId": 12, "message": "optional" }` | 201, `MatchRequestDto`. 409 if sending to yourself, to a connected student, or while a request is pending either way. 404 if the receiver does not exist | Student | Proposed |
| GET | `/api/match-requests?direction=incoming` | `direction` is `incoming` or `outgoing` | 200, list of `MatchRequestDto`, newest first, every status | Student | Proposed |
| POST | `/api/match-requests/{id}/accept` | none | 200, `MatchRequestDto` with status `ACCEPTED`. Creates the connection. 403 if you are not the receiver, 409 if no longer pending | Student | Proposed |
| POST | `/api/match-requests/{id}/decline` | none | 200, `MatchRequestDto` with status `DECLINED`. 403 if you are not the receiver, 409 if no longer pending | Student | Proposed |
| GET | `/api/connections` | none | 200, list of `ConnectionDto`, active connections only, newest first. Empty list if none | Student | Proposed |
| DELETE | `/api/connections/{id}` | none | 204. Ends the connection; from then on neither side sees the other's contact number. The other student is notified (`CONNECTION_ENDED`). 403 if you are not one of the two students, 404 if the connection does not exist, 409 if it has already ended | Student | Proposed |
| GET | `/api/students/{id}/profile` | none | 200, `ConnectedProfileDto` if you are that student or actively connected to them, otherwise `PublicProfileDto`. Decided on every request, so it flips back to public as soon as a connection ends. 404 if the student does not exist | Student | Proposed |
| GET | `/api/notifications` | none | 200, list of `NotificationDto`, newest first, read and unread. Empty list if none | Student | Proposed |
| GET | `/api/notifications/unread-count` | none | 200, `{ "count": 3 }` | Student | Proposed |
| POST | `/api/notifications/{id}/read` | none | 200, `NotificationDto` with `read: true`. Marking an already read notification again is not an error. 403 if it was sent to another student, 404 if it does not exist | Student | Proposed |
| POST | `/api/notifications/read-all` | none | 204. Marks every unread notification of yours as read | Student | Proposed |

`MatchRequestDto`:

```json
{
  "id": 10,
  "senderId": 1,
  "senderName": "Priya N.",
  "receiverId": 12,
  "receiverName": "Jamie Lee",
  "message": "Want to revise for the midterm?",
  "status": "PENDING",
  "createdAt": "2026-09-26T14:02:11"
}
```

No contact numbers. A request exists before any connection does. Each send, accept and
decline also creates a notification for the other student.

`ConnectionDto`, written from the caller's side:

```json
{
  "id": 7,
  "otherStudentId": 12,
  "otherStudentName": "Jamie Lee",
  "createdAt": "2026-09-27T09:12:40"
}
```

No contact number here. The contact number is only ever returned by the profile endpoint.

`PublicProfileDto`:

```json
{
  "id": 12,
  "name": "Jamie Lee",
  "school": "SCIS",
  "programme": "Information Systems",
  "yearOfStudy": 2,
  "coursesTaken": [
    { "id": 3, "code": "IS442", "name": "Object Oriented Programming" }
  ],
  "targetCourse": { "id": 3, "code": "IS442", "name": "Object Oriented Programming" },
  "preferredStudyMode": "IN_PERSON",
  "studyGoals": ["EXAM_PREPARATION"],
  "preferredGroupSizeMin": 2,
  "preferredGroupSizeMax": 3
}
```

`PublicProfileDto` has **no** `contactNumber` field, not even as `null`. `ConnectedProfileDto` is
the same shape plus one field:

```json
{
  "id": 12,
  "name": "Jamie Lee",
  "...": "every PublicProfileDto field",
  "contactNumber": "+65 9123 4567"
}
```

The frontend tells them apart by whether `contactNumber` is present. `coursesTaken` is sorted by
course code; `targetCourse`, `preferredStudyMode` and the group size fields may be `null`. The
group size is a min and max because that is how `Student` stores it today; it follows the open
group size question in `AGENTS.md`.

`NotificationDto`:

```json
{
  "id": 30,
  "type": "MATCH_REQUEST_ACCEPTED",
  "message": "Jamie Lee accepted your request. You can now see each other's contact number.",
  "read": false,
  "createdAt": "2026-09-26T15:40:02"
}
```

`type` is one of `MATCH_REQUEST_RECEIVED`, `MATCH_REQUEST_ACCEPTED`, `MATCH_REQUEST_DECLINED`,
`CONNECTION_ENDED`, `GROUP_JOIN_REQUEST_RECEIVED`, `GROUP_JOIN_REQUEST_ACCEPTED`,
`GROUP_JOIN_REQUEST_REJECTED`, `GROUP_MEMBER_REMOVED`.

Status codes assume Team B's exception handler maps `ConnectionNotFoundException`,
`StudentNotFoundException` and `NotificationNotFoundException` to 404,
`NotConnectionParticipantException` and `NotificationNotAllowedException` to 403, and
`IllegalStateException` to 409.

## Study groups (Team C)

Agreed by Team C: the leader counts toward `maxGroupSize` (they are stored as a member); members
cannot leave on their own yet, as the brief only asks for the leader removing members; a leader
cannot leave or be removed, they close the group instead.

| Method | Path | Request | Response | Auth | Status |
| ------ | ---- | ------- | -------- | ---- | ------ |
| GET | `/api/groups?courseId=3&studyGoal=EXAM_PREPARATION&studyMode=ONLINE` | Every filter is optional. `studyMode` `EITHER`, on the filter or on the group, matches any mode | 200, list of `StudyGroupSummaryDto`, open groups only, newest first | Student | Proposed |
| POST | `/api/groups` | `StudyGroupDetails` | 201, `StudyGroupDetailDto`. The creator becomes leader and first member. 400 if the details are invalid, 404 if the course does not exist | Student | Proposed |
| GET | `/api/groups/{id}` | none | 200, `StudyGroupDetailDto`, open or closed. 404 if the group does not exist | Student | Proposed |
| PUT | `/api/groups/{id}` | `StudyGroupDetails`; replaces every field, availability included | 200, `StudyGroupDetailDto`. 400 if invalid or `maxGroupSize` is below the current member count, 403 if you are not the leader, 404 if the group or course does not exist, 409 if the group is closed | Leader | Proposed |
| POST | `/api/groups/{id}/close` | none | 200, `StudyGroupDetailDto` with `active: false`. One-way. Every pending join request is rejected and its sender notified. 403 if you are not the leader, 409 if already closed | Leader | Proposed |
| DELETE | `/api/groups/{id}/members/{studentId}` | none | 204. The removed student is notified. 403 if you are not the leader, 409 if `studentId` is the leader or not a member | Leader | Proposed |

`StudyGroupDetails` (request body for create and update):

```json
{
  "name": "Midterm crammers",
  "description": "Weekly problem sets, optional",
  "courseId": 3,
  "studyGoals": ["EXAM_PREPARATION", "PROBLEM_SOLVING"],
  "preferredStudyMode": "IN_PERSON",
  "maxGroupSize": 4,
  "availability": [
    { "dayOfWeek": "MONDAY", "startTime": "18:00", "endTime": "20:00" }
  ]
}
```

`name`, `courseId` and `maxGroupSize` (at least 2: the leader plus one) are required. Missing
`studyGoals` or `availability` mean none. Each slot must start before it ends.

`StudyGroupSummaryDto` (browse list):

```json
{
  "id": 5,
  "name": "Midterm crammers",
  "courseId": 3,
  "courseCode": "IS442",
  "courseName": "Object Oriented Programming",
  "leaderId": 1,
  "leaderName": "Priya N.",
  "preferredStudyMode": "IN_PERSON",
  "studyGoals": ["EXAM_PREPARATION", "PROBLEM_SOLVING"],
  "maxGroupSize": 4,
  "memberCount": 2,
  "active": true
}
```

`StudyGroupDetailDto` has every summary field plus `description`, `createdAt`, `availability`
(same slot shape as above, Monday first) and `members`, in joining order:

```json
{ "studentId": 1, "name": "Priya N.", "leader": true, "joinedAt": "2026-10-01T09:30:00" }
```

No contact numbers anywhere in group responses. Being in the same group is not a connection.

### Group join requests (Team C)

Students ask to join a group; the leader accepts or rejects. This is a **second state machine**,
separate from `MatchRequest`. Do not try to reuse the same entity for both.

| Method | Path | Request | Response | Auth | Status |
| ------ | ---- | ------- | -------- | ---- | ------ |
| POST | `/api/groups/{id}/join-requests` | `{ "message": "optional" }` | 201, `GroupJoinRequestDto` with status `PENDING`. The leader is notified. 404 if the group does not exist, 409 if the group is closed or full, you are already a member, or you already have a pending request for it | Student | Proposed |
| GET | `/api/groups/{id}/join-requests` | none | 200, list of pending `GroupJoinRequestDto`, newest first. 403 if you are not the leader | Leader | Proposed |
| POST | `/api/groups/{id}/join-requests/{requestId}/accept` | none | 200, `GroupJoinRequestDto` with status `ACCEPTED`. Adds the student as a member and notifies them. 403 if you are not the leader, 404 if the request is not in this group, 409 if no longer pending, or the group is closed or full | Leader | Proposed |
| POST | `/api/groups/{id}/join-requests/{requestId}/reject` | none | 200, `GroupJoinRequestDto` with status `REJECTED`. The student is notified. 403 if you are not the leader, 404 if the request is not in this group, 409 if no longer pending | Leader | Proposed |

`GroupJoinRequestDto`:

```json
{
  "id": 20,
  "groupId": 5,
  "groupName": "Midterm crammers",
  "studentId": 12,
  "studentName": "Jamie Lee",
  "message": "Can I join for the finals?",
  "status": "PENDING",
  "createdAt": "2026-10-01T10:15:00"
}
```

Status codes assume Team B's exception handler maps `StudyGroupNotFoundException`,
`GroupJoinRequestNotFoundException` and `CourseNotFoundException` to 404,
`NotGroupLeaderException` to 403, `StudyGroupActionNotAllowedException` and
`IllegalStateException` to 409, and `InvalidStudyGroupException` to 400.

## Administration (Team C)

| Method | Path | Request | Response | Auth | Status |
| ------ | ---- | ------- | -------- | ---- | ------ |
Agreed by Team C: **deleting an account deactivates it**, never a hard delete. Deactivating a
student ends their active connections (the other student is notified), declines every match
request still pending to or from them (the other student is notified), closes every group they
lead (pending join requests are rejected and notified, as on a normal close), rejects their own
pending join requests to other groups (nobody is notified; the request leaves the leader's
pending list) and removes all of their group memberships. Already answered requests are kept as
history. Pending requests reuse `DECLINED` and `REJECTED` rather than a new status, so the
frontend sees no new values. Reactivating only lets the account sign in again; nothing ended by
deactivation is restored. **Usage information** means counts:
active connections, match requests sent (any status), groups led (open or closed) and groups
joined but not led, plus the account's `createdAt` and `active`. There is no last-login time.

| Method | Path | Request | Response | Auth | Status |
| ------ | ---- | ------- | -------- | ---- | ------ |
| GET | `/api/admin/users?role=STUDENT&active=true&search=jamie` | Every filter is optional. `role` is `STUDENT` or `ADMIN`; `search` matches part of the email or the student's name, ignoring case | 200, list of `AdminUserSummaryDto`, newest first, active and inactive. Empty list if nothing matches | Admin | Proposed |
| POST | `/api/admin/users` | TODO: `{ email, password, role }` plus `name`, `school`, `programme`, `yearOfStudy`, `contactNumber` when `role` is `STUDENT` | TODO: 201, `AdminUserDetailDto`; also creates the student profile; 409 if the email is taken. **Blocked: needs Team B's `PasswordEncoder` bean** | Admin | TODO |
| GET | `/api/admin/users/{id}` | none | 200, `AdminUserDetailDto`. 404 if the account does not exist | Admin | Proposed |
| PUT | `/api/admin/users/{id}` | `AdminUserUpdateRequest` | 200, `AdminUserDetailDto`. 400 if the email is missing or, for a student, a profile field is missing or `yearOfStudy` is below 1. 404 if the account does not exist, 409 if another account already uses the email | Admin | Proposed |
| DELETE | `/api/admin/users/{id}` | none | 200, `AdminUserDetailDto` with `active: false`. **Deactivates, does not delete**; see above for what happens to a student's connections and groups. 404 if the account does not exist, 409 if it is your own account or already deactivated | Admin | Proposed |
| POST | `/api/admin/users/{id}/reactivate` | none | 200, `AdminUserDetailDto` with `active: true`. 404 if the account does not exist, 409 if it is already active | Admin | Proposed |

`AdminUserUpdateRequest` (request body for update; active status is not changed here):

```json
{
  "email": "jamie.lee@smu.edu.sg",
  "name": "Jamie Lee",
  "school": "SCIS",
  "programme": "Information Systems",
  "yearOfStudy": 2,
  "contactNumber": "+65 9123 4567"
}
```

`email` is always required. The other fields are required for student accounts and ignored for
admin accounts.

`AdminUserSummaryDto` (list), with `name` `null` for admin accounts:

```json
{
  "id": 12,
  "email": "jamie.lee@smu.edu.sg",
  "role": "STUDENT",
  "name": "Jamie Lee",
  "active": true,
  "createdAt": "2026-09-01T10:00:00"
}
```

`AdminUserDetailDto` wraps the summary as `account` and adds `usage`, which is `null` for admin
accounts:

```json
{
  "account": {
    "id": 12,
    "email": "jamie.lee@smu.edu.sg",
    "role": "STUDENT",
    "name": "Jamie Lee",
    "active": true,
    "createdAt": "2026-09-01T10:00:00"
  },
  "usage": {
    "activeConnections": 2,
    "matchRequestsSent": 5,
    "groupsLed": 1,
    "groupsJoined": 3
  }
}
```

No password hash and no contact number in any admin response.

Status codes assume Team B's exception handler maps `UserNotFoundException` to 404,
`DuplicateEmailException`, `AdminActionNotAllowedException` and `IllegalStateException` to 409,
and `InvalidAdminUserException` to 400.

---

**Paths above are a starting proposal, not agreed.** They are here so the table has a shape to
argue with. Change them freely while the Status column still says TODO; once a row is agreed,
treat it as fixed.
