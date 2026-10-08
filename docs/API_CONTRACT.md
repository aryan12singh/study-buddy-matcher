# API contract

The Team C endpoints below are implemented on the Team C feature branch, with the controller
and DTO shapes the React frontend uses. Team A's matching and Team B's own-profile editor are
separate.

Base path `/api`; JSON requests/responses. Protected requests use
`Authorization: Bearer <token>`. Actor, student and administrator identity always comes from
the verified principal. Client-supplied acting-user fields do not confer authority.
Controllers validate input and call services; entities never become response bodies.

All event timestamps are UTC instants, serialized with `Z`. Weekly schedules are recurring
campus-local times in `Asia/Singapore`, with weekdays `MONDAY` through `SUNDAY` and whole-minute
`HH:mm`/`HH:mm:ss` times. Slots require `startTime < endTime`; empty schedules and overlapping
slots are allowed. The schema upgrade explicitly interprets legacy event timestamps using
the documented source timezone `Asia/Singapore`; it does not relabel them without conversion.

## Identity, permissions and errors

Login, registration and admin edits share trimmed, lowercase email identity. Its normalized
value is unique in PostgreSQL, including inactive accounts. Passwords require at least eight
characters and at most 72 UTF-8 bytes. Passwords are encoded with the existing Spring Security
BCrypt encoder. Secrets, hashes, token versions and stored contact data are absent from admin,
request, connection, group and notification responses.

JWTs include account ID, role, expiry and account token version. Every protected request checks
that the account still exists, is active, and matches the token role/version. Mutation services
recheck account state and the current principal's token version after locking. Deactivation
increments the version. Reactivation permits fresh login; old tokens stay invalid. Permanent
deletion invalidates all tokens by removing the account.

Errors use `application/problem+json` with RFC 9457 fields and stable form/application fields:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Check the highlighted fields",
  "instance": "/api/groups",
  "code": "VALIDATION_FAILED",
  "message": "Check the highlighted fields",
  "fieldErrors": { "name": "must not be blank" },
  "timestamp": "2026-10-07T02:00:00Z"
}
```

| HTTP | Code | Meaning |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | Annotated body validation; errors keyed by the field path |
| 400 | `INVALID_INPUT` | Invalid enum/JSON/query value or service field/size/time validation |
| 401 | `UNAUTHENTICATED` | Missing, invalid, expired, revoked token; missing/inactive current account |
| 401 | `INVALID_CREDENTIALS` | Login email/password does not identify an active account |
| 403 | `FORBIDDEN` | Wrong role, request receiver, connection participant, group leader or notification recipient |
| 404 | `NOT_FOUND` | Missing resource or unavailable/inactive student subject/target |
| 409 | `STATE_CONFLICT` | Duplicate pending relation, connected pair, repeated decision, closed/full group, self-removal |
| 409 | `DUPLICATE_EMAIL` | Normalized email already belongs to an account |
| 409 | `DATA_CONFLICT` | Database uniqueness/integrity protection rejected a conflicting write |
| 500 | `INTERNAL_ERROR` | Safe generic failure; no SQL, internal exception, credential or private data is returned |

Body and service field validation errors have `fieldErrors`; other errors use an empty object. Authentication
and permission errors share the same shape. All security responses include `Cache-Control:
no-store`; the profile controller sets no-store explicitly as well.

## Auth and course foundation

This reuses the fixed Spring Security/JJWT/JPA dependencies. Public registration creates a
student only. Account role selection belongs to the administrator creation endpoint.

| Method | Path | Request | Response | Auth |
| --- | --- | --- | --- | --- |
| POST | `/api/auth/login` | `{email,password}` | 200 `AuthResultDto` | Public |
| POST | `/api/auth/register` | `RegisterRequest` | 201 `AuthResultDto` | Public |
| GET | `/api/auth/me` | none | 200 `CurrentAccountDto` | Active account |
| GET | `/api/courses` | none | 200 `CourseDto[]`, course-code order | Student |
| GET | `/api/students/me/summary` | none | 200 `StudentActivityDto` | Student |

`RegisterRequest` requires `email`, `password`, `name`, `school`, `programme`, `yearOfStudy`
(integer >= 1) and `contactNumber`. Identity/profile strings are at most 255 characters.
Contact numbers are required nonblank text at creation, without a newly invented format rule.
Registration saves the identity and `@MapsId` student profile atomically and signs in the account.

```json
{
  "token": "<application JWT>",
  "expiresAt": "2026-10-08T02:00:00Z",
  "account": { "id": 12, "email": "jamie@demo.example.test", "role": "STUDENT", "name": "Jamie Lee" }
}
```

`CurrentAccountDto` is the nested `account` shape above, with `name: null` for administrators.
`CourseDto` is `{id,code,name}`. `StudentActivityDto` is
`{activeConnections,pendingIncoming,pendingOutgoing,acceptedGroups}`; acceptedGroups counts
current membership in open groups, including leadership. These real counts and the C request/
profile actions are reusable by Team A matching cards and Team B's dashboard.

Team A eligibility consumers can reuse `AccountAccess.eligibleStudent`, `requireStudent` and
`eligibleStudentIds` / `StudentRepository.findActiveIds`; existing and active STUDENT identities
are required. This contract does not provide scoring or ranking.

The full own-profile/preferences APIs (`GET/PUT /api/profile/me`,
`PUT /api/profile/me/availability`) remain Team B work.

## Buddy requests, connections and study profiles

| Method | Path | Request | Response | Auth |
| --- | --- | --- | --- | --- |
| POST | `/api/match-requests` | `SendMatchRequest` | 201 `MatchRequestDto` | Student |
| GET | `/api/match-requests/incoming` | none | 200 `MatchRequestDto[]`, all statuses newest first | Student |
| GET | `/api/match-requests/outgoing` | none | 200 `MatchRequestDto[]`, all statuses newest first | Student |
| POST | `/api/match-requests/{id}/accept` | none | 200 `MatchRequestDto`, `ACCEPTED` | Receiver |
| POST | `/api/match-requests/{id}/decline` | none | 200 `MatchRequestDto`, `DECLINED` | Receiver |
| GET | `/api/connections` | none | 200 `ConnectionDto[]`, active only newest first | Student |
| DELETE | `/api/connections/{id}` | none | 204 | Participant |
| GET | `/api/students/{id}/profile` | none | 200 `PublicProfileDto` or `ConnectedProfileDto` | Student |

The earlier `?direction=` request-list proposal is replaced by the explicit incoming/outgoing
paths above; there was no implemented consumer of the older proposal.

`SendMatchRequest`:

```json
{
  "receiverId": 12,
  "message": "Want to revise for the midterm?",
  "context": { "origin": "MATCHING", "courseId": 3, "studyGoal": "EXAM_PREPARATION" }
}
```

`receiverId` must be positive and identify an active student. Message is optional, at most 255
characters; blank becomes null. Context is optional and defaults to `{origin:"PROFILE"}`.
If supplied, origin is required (`PROFILE` or `MATCHING`). Optional course ID must identify an
existing course; optional study goal uses the enum below. Matching consumers supply known
search context; profile sends may omit it. Self-send, any reverse/forward pending request and
any active connection between the pair are 409. Inactive/missing recipients are 404.

```json
{
  "id": 10,
  "senderId": 1,
  "senderName": "Priya Nair",
  "receiverId": 12,
  "receiverName": "Jamie Lee",
  "message": "Want to revise for the midterm?",
  "status": "PENDING",
  "createdAt": "2026-10-07T02:00:00Z",
  "respondedAt": null,
  "context": {
    "origin": "MATCHING",
    "courseId": 3,
    "courseCode": "IS442",
    "courseName": "Object Oriented Programming",
    "studyGoal": "EXAM_PREPARATION"
  }
}
```

Nullable course/context values remain explicit nulls. Request status is `PENDING`, `ACCEPTED`,
`DECLINED` or `CANCELLED`. `CANCELLED` means nobody answered because one of the two accounts was
deactivated or deleted. Decisions are one-way. Acceptance creates exactly one symmetric active
connection. Repeated decisions produce a conflict and no duplicate event. Answered history
and ended connections remain available to admin counts and request history; ended connections
are not returned by the connection list.

`ConnectionDto` is `{id,otherStudentId,otherStudentName,createdAt}`, written from the caller's
side. It has no contact number. Disconnect is one-way and notifies the other participant.

`PublicProfileDto`:

```json
{
  "id": 12,
  "name": "Jamie Lee",
  "school": "SCIS",
  "programme": "Information Systems",
  "yearOfStudy": 2,
  "coursesTaken": [{ "id": 3, "code": "IS442", "name": "Object Oriented Programming" }],
  "targetCourse": null,
  "preferredStudyMode": "IN_PERSON",
  "studyGoals": ["EXAM_PREPARATION"],
  "preferredGroupSizeMin": 2,
  "preferredGroupSizeMax": 3,
  "availability": [{ "dayOfWeek": "MONDAY", "startTime": "18:00:00", "endTime": "20:00:00" }],
  "relationship": { "state": "INCOMING_PENDING", "requestId": 10, "connectionId": null }
}
```

The connected DTO has every public field plus `contactNumber`. Public DTOs have no contact
field, including no null placeholder. Only self or an active accepted buddy gets the connected
shape. Group membership and leadership grant no contact access. Pending, declined, stranger
and disconnected viewers get the public shape; every new view checks the current relationship.
Inactive/missing subjects return 404. Public course order is by code; schedules are ordered
Monday first then by start time. Optional preference/target-course fields may be null.

Relationship state is `SELF`, `STRANGER`, `INCOMING_PENDING`, `OUTGOING_PENDING` or `CONNECTED`.
A pending state includes its request ID; connected includes its connection ID; irrelevant IDs
are null. The existing numeric preferred-group-size storage remains a Team A decision, not
an additional C scoring interpretation.

## Groups and membership applications

`StudyMode`: `IN_PERSON`, `ONLINE`, `EITHER`. `StudyGoal`: `CONCEPT_REVIEW`, `PROBLEM_SOLVING`,
`EXAM_PREPARATION`, `PROJECT_DISCUSSION`.

| Method | Path | Request | Response | Auth |
| --- | --- | --- | --- | --- |
| GET | `/api/groups?courseId=&studyGoal=&studyMode=` | Optional typed filters | 200 `StudyGroupSummaryDto[]`, open only newest first | Student |
| GET | `/api/groups/mine` | none | 200 summaries of current memberships or led groups, including closed history | Student |
| POST | `/api/groups` | `StudyGroupDetails` | 201 `StudyGroupDetailDto` | Student |
| GET | `/api/groups/{id}` | none | 200 `StudyGroupDetailDto`, open or closed | Student |
| PUT | `/api/groups/{id}` | `StudyGroupDetails`, all editable fields replaced | 200 detail | Leader |
| POST | `/api/groups/{id}/close` | none | 200 detail, `active:false` | Leader |
| DELETE | `/api/groups/{id}/members/{studentId}` | none | 204 | Leader |
| POST | `/api/groups/{id}/join-requests` | `{message?:string}` | 201 `GroupJoinRequestDto` | Student |
| GET | `/api/groups/{id}/join-requests` | none | 200 pending application DTOs, newest first | Leader |
| POST | `/api/groups/{id}/join-requests/{requestId}/accept` | none | 200 application DTO, `ACCEPTED` | Leader |
| POST | `/api/groups/{id}/join-requests/{requestId}/reject` | none | 200 application DTO, `REJECTED` | Leader |
| GET | `/api/group-join-requests/mine` | none | 200 own application DTOs, every status newest first | Student |

A leader is a relationship to one group, not an account role. The creator becomes the first
accepted member and counts toward capacity. Pending applications do not count. No self-leave,
leader transfer or group reopening is added.

```json
{
  "name": "Midterm crammers",
  "description": "Weekly problem sets",
  "courseId": 3,
  "studyGoals": ["EXAM_PREPARATION", "PROBLEM_SOLVING"],
  "preferredStudyMode": "IN_PERSON",
  "maxGroupSize": 4,
  "availability": [{ "dayOfWeek": "MONDAY", "startTime": "18:00", "endTime": "20:00" }]
}
```

Name is required and <=255 characters. Course ID must be positive/existing. Maximum size is
required and >=2; reduction below current accepted membership is 400. Description is optional
and <=4000 characters; blank becomes null. Mode is optional. Missing/null goals and availability
mean empty collections. Null collection entries, missing slot values and reversed/equal times
are invalid. Slots retain their campus-local meaning; no dated sessions are introduced.

Browse filters are combined with AND. `EITHER` on a mode filter or group is compatible with
any mode. Your groups is unaffected by browse filters and includes closed groups led by the
caller even when deactivation removed their membership.

Summary fields: `id,name,courseId,courseCode,courseName,leaderId,leaderName,preferredStudyMode,
studyGoals,maxGroupSize,memberCount,active,viewer`. `viewer` is
`{leader:boolean,member:boolean,requestId:number|null,requestStatus:status|null}` and reports
the caller's latest application. Detail adds `description,createdAt,availability,members`.
Member shape is `{studentId,name,leader,joinedAt}` in joining order. No group response carries
contact data. The agenda UI uses saved goals and availability from detail.

`GroupJoinRequestDto`: `{id,groupId,groupName,studentId,studentName,message,status,createdAt,
respondedAt,groupActive}`. Status is `PENDING`, `ACCEPTED`, `REJECTED`. Message rules match
buddy requests. Creation rejects closed/full groups, membership already present or a pending
application with 409. Approval rechecks pending state, active applicant/group, membership and
capacity under locks. A request ID outside the URL's group is 404; a different group's leader
has no authority. Repeated decisions are 409. Editing, approving, removing and rejecting are
blocked on closed groups. Closure rejects every pending application once and notifies current
members other than the leader. The leader cannot be removed.

## Private study room foundation (E2 #16)

Accepted active student members of an open group can read its room. Pending
applicants, non-members and admins cannot enter. A GET returns default state
without creating a row; the first join/control/settings write creates one.
Every route rechecks eligibility. Room payloads omit contacts, emails and tokens.

| Method | Path | Input / result |
| --- | --- | --- |
| GET | `/api/groups/{groupId}/room` | Current `StudyRoomDto`, 200 |
| POST | `/api/groups/{groupId}/room/join` | `{clientId: UUID}`; idempotent lease, DTO, 200 |
| PUT | `/api/groups/{groupId}/room/presence` | `{clientId: UUID, presence: PRESENT / FOCUS / BREAK}`; renewed DTO, 200 |
| DELETE | `/api/groups/{groupId}/room/presence/{clientId}` | Delete only the actor's matching tab lease; idempotent, 204 |
| POST | `/api/groups/{groupId}/room/timer` | `{command: START / PAUSE / RESUME / RESET, expectedVersion}`; DTO, 200 |
| PUT | `/api/groups/{groupId}/room/audio` | `{preset: CALM_MUSIC / RAIN / WHITE_NOISE / CAFE, playing: boolean, expectedVersion}`; DTO, 200 |
| PUT | `/api/groups/{groupId}/room/settings` | `{focusMinutes, breakMinutes, participantLimit, hostId?, coHostId?, expectedVersion}`; DTO, 200 |

`expectedVersion` is a non-negative integer matching the current room version.
Shared changes increment it; presence renewals do not. A stale command or invalid
timer transition returns 409. Only the leader or a currently present assigned
host/co-host can change timer/audio. The leader retains these controls without
joining, so room capacity cannot block administrative recovery. Only the leader
changes settings/roles. Null `hostId` uses the leader; null `coHostId` means none.
Assigned IDs must be active accepted members and distinct after that fallback.

Durations are positive whole minutes, bounded by the configured maxima. Changing
durations requires IDLE (reset first); roles/capacity can change while running.
Capacity is positive, at most the current group capacity and at least current
distinct present students. Concurrent joins for the last place serialize; one
gets 409. Multiple tabs from one student count once. The client UUID is scoped
to the authenticated actor, not an authorization credential.

Each heartbeat extends its lease by the configured lifetime. An unknown/expired
lease returns 409 and requires an explicit join; it cannot bypass capacity by
renewing. Inactive/removed members are excluded immediately; closed groups return
409. The timer continues across disconnects and repeating focus/break phases.
A removed/deleted/inactive host falls back to the leader, and invalid co-hosts
are omitted. Account deletion cascades presences; leader/group deletion removes
the room. Clients purge snapshots/stop audio on refresh failure, then retry and
explicitly rejoin. Browser timer rendering does not confer control authority.

`StudyRoomDto` fields:

```text
groupId, groupName, version, serverTime (UTC Instant)
focusMinutes, breakMinutes, maxFocusMinutes, maxBreakMinutes
participantLimit (effective, capped at current groupLimit), groupLimit
hostId, coHostId (nullable), hostOnline
leader, canControl, joined (actor present in any tab)
pollIntervalMillis, leaseLifetimeMillis
timer {phase: FOCUS/BREAK, status: IDLE/RUNNING/PAUSED,
       remainingMillis (at serverTime), focusMillis, breakMillis}
audio {preset, playing}
audioPresets [{id, label, kind: MUSIC/AMBIENT}]
participants [{studentId, name, presence, expiresAt, leader, host, coHost}]
members [{studentId, name}] (active accepted members for role selectors)
```

The browser renders elapsed time from each server snapshot and reconciles on the
server-provided poll interval (default two seconds). Local enable/volume/mute are
not sent to the API. Original synthesized audio shares a selection/play state,
without synchronizing playback positions. No external music credentials/assets
are required. Dated sessions/calendar are a later slice, with no placeholder APIs.

## Notifications

| Method | Path | Request | Response | Auth |
| --- | --- | --- | --- | --- |
| GET | `/api/notifications?filter=ALL` | `ALL`, `REQUESTS`, `GROUPS`; defaults ALL | 200 `NotificationDto[]`, newest first | Student |
| GET | `/api/notifications/unread-count` | none | 200 `{count:number}` | Student |
| POST | `/api/notifications/{id}/read` | none | 200 notification with `read:true` | Recipient |
| POST | `/api/notifications/read-all` | none | 204 | Student |

Notification DTO: `{id,type,message,read,createdAt,resourceType,resourceId,eventKey,requestDirection}`.
`resourceType` is `MATCH_REQUEST`, `GROUP`, `STUDENT` or null. Resource ID and type are both null
for safe generic deletion notices and older notices without metadata. `eventKey` is a nullable
stable event identity, never an authentication token. MATCH_REQUEST links to requests/
connections; GROUP links to the group; STUDENT links to its public/connected profile. Opening
a target still uses its authorized endpoint. A now unavailable target returns a safe 404;
notification metadata does not confer permissions.

`requestDirection` is `INCOMING`, `OUTGOING` or null. For a `MATCH_REQUEST` resource it follows
the stored participants and notification recipient: the receiver's event links to Incoming,
the sender's event links to Outgoing. A cancellation after sender deactivation therefore
remains `INCOMING` for the receiver. Ordinary acceptance/decline events to the sender are
`OUTGOING`. Event type and message text do not determine direction. Other resource types,
generic notices and missing or unrelated request targets have null direction.

REQUESTS includes buddy-request and disconnect events. GROUPS includes all `GROUP_*` events.
Unread count always covers all categories, regardless of list filtering. Read-one/read-all
are idempotent and scoped to the current recipient. Storage uses text for summaries so names
and descriptions at their valid limits cannot cause a notification-column overflow.

| Successful event | Recipients and count | Notification type |
| --- | --- | --- |
| Buddy send | Receiver, one | `MATCH_REQUEST_RECEIVED` |
| Accept / decline | Sender, one | `MATCH_REQUEST_ACCEPTED` / `MATCH_REQUEST_DECLINED` |
| Request cancelled by deactivation or deletion | Other participant, one | `MATCH_REQUEST_CANCELLED` |
| Disconnect | Other participant, one | `CONNECTION_ENDED` |
| Group application | Leader, one | `GROUP_JOIN_REQUEST_RECEIVED` |
| Group accept / reject | Applicant, one | `GROUP_JOIN_REQUEST_ACCEPTED` / `GROUP_JOIN_REQUEST_REJECTED` |
| Member removal | Removed member, one | `GROUP_MEMBER_REMOVED` |
| Group closure | Each pending applicant, one rejection; each accepted member other than leader, one closure | `GROUP_JOIN_REQUEST_REJECTED`, `GROUP_CLOSED` |
| Student deactivation | Each active buddy and pending buddy counterpart; led-group closure recipients; leaders of withdrawn applications and removed memberships | Corresponding ended/cancelled/rejected/removed events; the ended-connection notice has no profile link |
| Permanent deletion | Active buddies, pending buddy counterparts, each remaining member/pending applicant in deleted led groups, and leaders affected by removed membership/application | Same safe event types; deleted target references removed |

Failed/conflicting domain actions create no notification. Locked transitions and the partial
unique `(recipient_id,event_key)` index protect event identity. Notifications and domain changes
commit or roll back together. Required closure notifications are not replaced by ordinary
student actions that would reject the now inactive account.

## Administrator accounts

This replaces the earlier Delete-as-deactivate proposal. Deactivate and Delete
permanently are distinct API and UI actions. Role is selected at creation and read-only during
ordinary editing. Password reset and Student/Admin conversions are separate requirements.

| Method | Path | Request | Response | Auth |
| --- | --- | --- | --- | --- |
| GET | `/api/admin/users?role=&active=&search=` | Optional role/status/search | 200 `AdminUserSummaryDto[]`, newest first | Admin |
| GET | `/api/admin/users/summary` | none | 200 `AdminAccountsSummaryDto` | Admin |
| POST | `/api/admin/users` | `AdminUserCreateRequest` | 201 `AdminUserDetailDto` | Admin |
| GET | `/api/admin/users/{id}` | none | 200 detail | Admin |
| PUT | `/api/admin/users/{id}` | `AdminUserUpdateRequest` | 200 detail | Admin |
| POST | `/api/admin/users/{id}/deactivate` | none | 200 detail, `active:false` | Admin |
| POST | `/api/admin/users/{id}/reactivate` | none | 200 detail, `active:true` | Admin |
| DELETE | `/api/admin/users/{id}` | none | 204, permanent deletion | Admin |

Search is a case-insensitive substring of email or student name. Role is STUDENT/ADMIN;
active is a Boolean. Summary `{total,active,inactive,students,admins}` counts all saved accounts
and does not depend on the filtered list.

Creation body: `{email,password,role,name?,school?,programme?,yearOfStudy?,contactNumber?}`.
Student creation requires all profile fields; administrator creation requires email/password/
role only. User and student creation is atomic. Email/password/profile limits match registration.

Update body: `{email,name?,school?,programme?,yearOfStudy?,contactNumber?}`. Email is always
required; name/school/programme/year are required for students. Contact replacement is optional:
omitted/null preserves the stored value; a supplied blank replacement is invalid. Admin editing
requires email only. Status, role and password are not edited through this endpoint.

Summary fields: `{id,email,role,name,active,createdAt,lastLoginAt}`. Last login is null for
"Never logged in". Registration's successful automatic sign-in counts as its first login;
subsequent successful login updates it. Failed login and token use do not update it. Name is null for admins. Detail shape:

```json
{
  "account": {
    "id": 12, "email": "jamie@demo.example.test", "role": "STUDENT", "name": "Jamie Lee",
    "active": true, "createdAt": "2026-10-07T02:00:00Z", "lastLoginAt": null
  },
  "profile": { "name": "Jamie Lee", "school": "SCIS", "programme": "Information Systems", "yearOfStudy": 2 },
  "usage": { "activeConnections": 2, "matchRequestsSent": 5, "groupsLed": 1, "groupsJoined": 3, "acceptedGroups": 4 }
}
```

Admin accounts have `profile:null,usage:null`. Contact is not returned to administrators.
Usage definitions: activeConnections = current unended buddies; matchRequestsSent = all sent
history; groupsLed = all led groups including closed; groupsJoined = current memberships in
groups the student does not lead, including closed; acceptedGroups = current membership in
open groups including leadership. Counts are real; absent relations produce zero.

Deactivation retains account/profile/history, revokes access, ends active connections,
cancels pending buddy requests both ways, closes led groups and rejects their pending
applicants, rejects the student's pending applications and removes their memberships in open
groups they do not lead. Closed groups keep their members, including the leader, as history. Reactivation restores access through fresh login only; it restores no
ended relation or membership. Repeated status operations are 409.

Permanent deletion removes the account/student, their buddy requests/connections, memberships,
availability, enrollment/goals and recipient notifications. Groups they lead are removed with
all dependent rows; groups led by others remain. Affected remaining users receive safe events.
Notifications linked to deleted student/request/group records are removed; replacement generic
notices contain no broken target. The email becomes available again after permanent deletion.

Self-deactivation/deletion is blocked. Because the acting admin must be active, at least one
active administrator always remains. Account removals/status changes serialize in PostgreSQL
and lock administrators in fixed ID order, then recheck the acting admin. An actor concurrently
revoked cannot finish a privileged mutation. All cleanup, notifications and identity changes form one transaction.

## Database transaction protocol

Every application write takes the shared PostgreSQL transaction advisory lock `4422026` before
reading mutable account/resource state. Account lifecycle and opt-in seed take its exclusive
variant. This prevents lifecycle cleanup racing with a new relationship in another process.
Ordinary writes then lock affected user rows in ascending ID order, followed by group and
request/connection rows, and revalidate state after waiting. Group capacity edits/removal/
closure and approval share the group lock. No JVM-only locks stand in for this protocol.

Login locks the normalized email lookup before loading the account. If an administrator's
email edit commits while that lookup waits, the old email is rejected and cannot overwrite the
edited identity when recording a successful login.

Partial unique indexes protect unordered pending buddy pairs, unordered active connection
pairs, pending `(study_group_id,student_id)` applications and `(recipient_id,event_key)` notices.
Membership `(study_group_id,student_id)` stays unique. Answered/ended history remains legal.
The migrations create these constraints; Hibernate `update` does not. RLS and browser-role privilege removal
protect the alternate Supabase access path; operational evidence is recorded separately.

## Team A integration surface still pending

| Method | Path | Owning work remaining |
| --- | --- | --- |
| GET | `/api/matches` | Team A scoring/ranking: course or study-goal entry point, additional filters/strategy/threshold and per-criterion MatchScore |
| GET | `/api/admin/matching-config` | Team A current weights/threshold/active strategy |
| PUT | `/api/admin/matching-config` | Team A validation and persistence of matching settings |

These are proposals, not implemented C endpoints. Reuse C profile privacy, active-student
eligibility and structured match-request context when connecting the matching screen.

## UI use of the existing contract

The UI polish adds no endpoint or DTO field. Group capacity/leader/member/application
indicators use the existing `memberCount`, `maxGroupSize`, `active` and `viewer` fields;
they never infer membership from a local click or reserve a place for a pending request.
Schedule duplicate/copy produces the same weekly-slot array accepted by POST/PUT groups;
the saved detail read supplies the schedule/agenda afterward. Existing group validation and
transactional capacity checks still apply.

Browser query state is distinct from API input: `/groups?view=mine` reads groups/mine,
`view=applications` reads group-join-requests/mine; browse passes only valid `courseId`,
`studyGoal` and `studyMode` filters. `/notifications?view=requests|groups` maps to the
existing REQUESTS/GROUPS filter, and the default is ALL. Other URL keys are preserved by
the UI but never added to API requests. Connection notification links retain their existing
incoming/outgoing view contract.

Copy contact is offered only for a supplied self/CONNECTED profile contact; group membership
and administrator privileges do not grant it. The same privacy assembler and profile
no-store/session-purge rules govern that read. Group-link copy uses the client origin plus
`/groups/{id}` and drops query/hash. Clipboard denial has a manual fallback, not another API.
Create/update/delete destination feedback contains generic outcome text and no DTO/private
fields.
