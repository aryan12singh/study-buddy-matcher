# API contract

This contract covers the integrated compulsory Team A/B/C application and the DTO/input
shapes used by the React frontend. Selected E1/E2 extensions are separate work.

Base path `/api`; JSON requests/responses. Protected requests use
`Authorization: Bearer <token>`. Actor, student and administrator identity always comes from
the verified principal. Client-supplied acting-user fields do not confer authority.
Controllers validate input and call services; entities never become response bodies.

All event timestamps are UTC instants, serialized with `Z`. Weekly schedules are recurring
campus-local times in `Asia/Singapore`, with weekdays `MONDAY` through `SUNDAY` and whole-minute
`HH:mm`/`HH:mm:ss` times. Slots require `startTime < endTime`; empty schedules are allowed.
Own student availability rejects same-day overlaps; group schedules permit overlaps. The
schema upgrade explicitly interprets legacy event timestamps using
the documented source timezone `Asia/Singapore`; it does not relabel them without conversion.

Weekly `LocalTime` mappings use JDBC's direct local-time type for both personal and
group slots. Their stored/displayed `10:00` remains `10:00` under Singapore or UTC
JVM defaults; an instant/timezone conversion must not relabel these civil-time
preferences. E2's `LocalDateTime` session fields likewise stay local civil values;
their separately generated UTC occurrence fields carry timezone resolution.

## Identity, permissions and errors

Login, registration and admin edits share trimmed, lowercase email identity. Its normalized
value is unique in PostgreSQL, including inactive accounts. New passwords require at least
eight characters and at most 72 UTF-8 bytes. Login retains annotated request validation;
otherwise-valid input exceeding the UTF-8 byte limit gets the same 401 `INVALID_CREDENTIALS`
as a wrong password before BCrypt comparison. Malformed/over-character-limit fields still
return 400. After `app.auth.login-throttle.max-failures` wrong passwords in a row for one
normalized email, known or unknown, login for it returns 429 `TOO_MANY_LOGIN_ATTEMPTS` for
`app.auth.login-throttle.lockout` after the last attempt; a successful login clears the count.
Each attempt is counted before its password is checked, so parallel guesses cannot exceed it. Exact text and existing shorter-password compatibility are retained. Secrets, hashes, token versions and stored contact data are absent from admin,
request, connection, group and notification responses.

Contact input accepts local Singapore 8-digit numbers beginning 6/8/9, or `+` followed by
7–15 international digits beginning 1–9. ASCII whitespace, hyphens, parentheses and dots are removed;
local input gains `+65`. Invalid replacements return 400 with a `contactNumber` field error.
The rule applies to registration, own-profile replacement and admin creation/replacement;
omitted admin contact preserves the stored value. Existing values are not bulk rewritten,
and administrator responses still omit contacts. This is format, not reachability, validation.

The team interprets learning style through the existing defined preferences; no separate
field is part of this baseline contract. This records the team decision, not professor confirmation.

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
| 401 | `INVALID_CREDENTIALS` | Login email/password does not match an account (checked before account state) |
| 403 | `ACCOUNT_DEACTIVATED` | Login with the correct password for a deactivated account |
| 429 | `TOO_MANY_LOGIN_ATTEMPTS` | Login for an email that reached the wrong-password limit; refused even with the right password until the lockout passes |
| 403 | `FORBIDDEN` | Wrong role, request receiver, connection participant, group leader or notification recipient |
| 404 | `NOT_FOUND` | Missing resource or unavailable/inactive student subject/target |
| 409 | `STATE_CONFLICT` | Duplicate pending relation, connected pair, repeated or stale decision, closed/full group, self-removal, leader leaving |
| 409 | `DUPLICATE_EMAIL` | Normalized email already belongs to an account |
| 409 | `STALE_MATCHING_CONFIG` | Expected configuration revision no longer matches; reload before saving/restoring |
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
| POST | `/api/auth/logout` | none | 204 | Active account; increments the account's token version, so every token issued to it so far (all tabs and devices) gets 401 |
| GET | `/api/courses` | none | 200 `CourseDto[]`, course-code order | Student |
| GET | `/api/students/me/summary` | none | 200 `StudentActivityDto` | Student |
| GET | `/api/achievements/me` | none | 200 `AchievementsDto` | Student |

`RegisterRequest` requires `email`, `password`, `name`, `school`, `programme`, `yearOfStudy`
(integer >= 1) and `contactNumber`. Identity/profile strings are at most 255 characters.
Contact numbers are required at creation and follow the accepted format/normalization contract above.
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

## Own profile and preferences (Team B)

| Method | Path | Request | Response | Auth |
| --- | --- | --- | --- | --- |
| GET | `/api/profile/me` | none | 200 `MyProfileDto` | Student |
| PUT | `/api/profile/me` | `UpdateProfileRequest`; replaces the whole profile | 200 `MyProfileDto` | Student |
| PUT | `/api/profile/me/availability` | `{ "slots": [AvailabilitySlot] }`; replaces the whole week, `[]` clears it | 200 `[AvailabilitySlot]`, Monday first | Student |

```
MyProfileDto {
  id, email, name, school, programme, yearOfStudy, contactNumber,   // own profile, so contact is included
  coursesTaken: [{ id, code, name }], targetCourse: { id, code, name } | null,
  preferredStudyMode: "IN_PERSON" | "ONLINE" | "EITHER",
  groupSizePreference: "ONE_TO_ONE" | "SMALL_GROUP" | "EITHER",
  groupSizeMax,                                                        // largest group, from app.profile.group-size-max
  studyGoals: ["CONCEPT_REVIEW" | "PROBLEM_SOLVING" | "EXAM_PREPARATION" | "PROJECT_DISCUSSION"],
  availability: [AvailabilitySlot]
}
UpdateProfileRequest {
  name, school, programme, yearOfStudy, contactNumber,                // required, as at registration
  courseIds: [id],                                                     // at least 1, at most app.profile.max-courses
  targetCourseId: id | null,                                           // need not be one of courseIds
  preferredStudyMode, groupSizePreference,                             // required
  studyGoals: [goal]                                                   // optional
}
AvailabilitySlot { dayOfWeek: "MONDAY".."SUNDAY", startTime: "HH:mm", endTime: "HH:mm" }
```

Group size is stored on `students` as a minimum and maximum: one-to-one = 2-2, small group =
3-max, either = 2-max. Availability blocks must start before they end, use whole minutes and not
overlap on the same day; there is no limit on how many. Invalid input returns 400
`INVALID_INPUT` with `fieldErrors` keyed by the request field (`courseIds`, `availability`, ...).

The shared editor keeps raw time drafts separate from this complete-slot payload. Invalid or
incomplete text blocks profile save and participates in unsaved-change protection; it must
not silently submit a previous valid time. An empty saved `slots` array remains valid. This
draft state is local form state, not an added API/DTO field.

## Buddy requests, connections and study profiles

| Method | Path | Request | Response | Auth |
| --- | --- | --- | --- | --- |
| POST | `/api/match-requests` | `SendMatchRequest` | 201 `MatchRequestDto` | Student |
| GET | `/api/match-requests/incoming` | none | 200 `MatchRequestDto[]`, all statuses newest first | Student |
| GET | `/api/match-requests/outgoing` | none | 200 `MatchRequestDto[]`, all statuses newest first | Student |
| POST | `/api/match-requests/{id}/accept` | none | 200 `MatchRequestDto`, `ACCEPTED` | Receiver |
| POST | `/api/match-requests/{id}/decline` | none | 200 `MatchRequestDto`, `DECLINED` | Receiver |
| POST | `/api/match-requests/{id}/cancel` | none | 200 `MatchRequestDto`, `CANCELLED` | Sender |
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
`DECLINED` or `CANCELLED`. `CANCELLED` means nobody answered: the sender withdrew the request,
or one of the two accounts was deactivated or deleted. Decisions are one-way, and a withdrawn
request frees the pair to send a new one. Acceptance creates exactly one symmetric active
connection. Repeated decisions produce a conflict and no duplicate event. Answered history
and ended connections remain available to admin counts and request history; ended connections
are not returned by the connection list.

Withdrawing does not erase the request: the recipient sees its `CANCELLED` status under
Past requests and cannot accept it. UI confirmations capture the original request/connection
and signed-in account; navigation or a refreshed replacement relation requires fresh consent.

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
are null. The categorical profile choice maps to Team A's numeric range scoring contract;
profile viewing does not introduce another scoring interpretation.

The owner's profile editor keeps independently saved profile/week drafts across recoverable
profile/course refresh errors. Retry does not discard the other draft. Lost authorization,
account changes and explicit session purge clear it; this does not retain another student's
private profile after a failed refresh.

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
| POST | `/api/groups/{id}/leave` | none | 204; 409 for the leader, a non-member or a closed group | Member |
| POST | `/api/groups/{id}/join-requests` | `{message?:string}` | 201 `GroupJoinRequestDto` | Student |
| GET | `/api/groups/{id}/join-requests` | none | 200 pending application DTOs, newest first | Leader |
| POST | `/api/groups/{id}/join-requests/{requestId}/accept` | none | 200 application DTO, `ACCEPTED` | Leader |
| POST | `/api/groups/{id}/join-requests/{requestId}/reject` | none | 200 application DTO, `REJECTED` | Leader |
| GET | `/api/group-join-requests/mine` | none | 200 own application DTOs, every status newest first | Student |
| POST | `/api/group-join-requests/{requestId}/withdraw` | none | 200 application DTO, `WITHDRAWN`; 404 for another student's application | Applicant |

A leader is a relationship to one group, not an account role. The creator becomes the first
accepted member and counts toward capacity. Pending applications do not count. A member may
leave an open group; the leader closes the group instead, and a closed group keeps its members as
history. An applicant may withdraw a pending application and apply again later. Application
status is `PENDING`, `ACCEPTED`, `REJECTED` (the leader's answer or closure) or `WITHDRAWN` (the
applicant's choice, or their account was deactivated or deleted). Leader transfer and group
reopening are not added.

Group confirmations likewise bind to the original group, application or membership tenure.
Changing the route/account, replacing a pending application, or leaving and rejoining clears
an earlier confirmation. An unchanged refresh preserves the current confirmation. Leaving
revokes room presence/roles and private session/calendar access in the membership transaction;
the group's sessions remain available to its remaining members.

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
are invalid. Valid group slots may overlap on the same day, unlike own student availability.
The group form must block invalid/incomplete raw time drafts and include them in exit
protection; overlap permission does not relax start/end or whole-minute validity. Slots retain
their campus-local meaning; dated E2 sessions use the separate session endpoints.

Browse filters are combined with AND. `EITHER` on a mode filter or group is compatible with
any mode. Your groups is unaffected by browse filters and includes closed groups led by the
caller even when deactivation removed their membership.

Summary fields: `id,name,courseId,courseCode,courseName,leaderId,leaderName,preferredStudyMode,
studyGoals,maxGroupSize,memberCount,active,viewer`. `viewer` is
`{leader:boolean,member:boolean,requestId:number|null,requestStatus:status|null}` and reports
the caller's latest application. Detail adds `description,createdAt,availability,members,
pendingApplications`; `pendingApplications` is the number of applications awaiting a decision
for the leader and `null` for every other caller.
Member shape is `{studentId,name,leader,joinedAt}` in joining order. No group response carries
contact data. The agenda UI uses saved goals/weekly availability from detail and
separately loads authorized dated E2 sessions.

`GroupJoinRequestDto`: `{id,groupId,groupName,studentId,studentName,message,status,createdAt,
respondedAt,groupActive}`. Status is `PENDING`, `ACCEPTED`, `REJECTED`. Message rules match
buddy requests. Creation rejects closed/full groups, membership already present or a pending
application with 409. Approval rechecks pending state, active applicant/group, membership and
capacity under locks. A request ID outside the URL's group is 404; a different group's leader
has no authority. Repeated decisions are 409. Editing, approving, removing and rejecting are
blocked on closed groups. Closure rejects every pending application once and notifies current
members other than the leader. The leader cannot be removed.

## Private study room (E2 #16)

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
| PUT | `/api/groups/{groupId}/room/audio` | `{musicPreset, musicPlaying, ambientPreset, ambientPlaying, expectedVersion}`; DTO, 200 |
| PUT | `/api/groups/{groupId}/room/settings` | `{focusMinutes, breakMinutes, participantLimit, hostId?, coHostId?, expectedVersion}`; DTO, 200 |

`expectedVersion` is a non-negative integer matching the current room version.
Shared changes increment it; presence renewals do not. A stale command or invalid
timer transition returns 409. Only the leader or a currently present assigned
host/co-host can change timer/audio/settings. The leader retains these controls without
joining, so room capacity cannot block administrative recovery. Only the leader
assigns roles; a present delegated host/co-host can change durations/capacity only
while preserving the effective current role assignments in the settings body.
Attempting to reassign roles as a delegated host returns 403. Null `hostId` uses
the leader; null `coHostId` means none.
Assigned IDs must be active accepted members and distinct after that fallback.

Durations are positive whole minutes, bounded by the configured maxima. Changing
durations requires IDLE (reset first); roles/capacity can change while running.
Capacity is positive, at most the current group capacity and at least current
distinct present students. Concurrent joins for the last place serialize; one
gets 409. Multiple tabs from one student count once. The client UUID is scoped
to the authenticated actor, not an authorization credential.

Each heartbeat extends its lease by the configured lifetime. An unknown/expired
lease returns 409 and requires an explicit join; it cannot bypass capacity by
renewing. Polled state reads use `RoomAccess.readMember` without a group-row lock.
Heartbeats use the same authorization check and update only an existing, unexpired
tab lease; a concurrently deleted lease fails renewal. Join, leave, timer/audio
control and configuration use `lockMember`, taking account locks before the group
lock. This serializes admission and shared-state transitions without queuing every
poll or lease renewal on the group row. Inactive/removed members are excluded; closed groups return
409. The timer continues across disconnects and repeating focus/break phases.
A removed/deactivated member loses all tab leases and assigned roles within the
membership/account transaction; later readmission/reactivation cannot revive an
old heartbeat. A removed/deleted/inactive host falls back to the leader, and invalid
co-hosts are omitted. Group closure removes leases, resets the timer and stops both
audio layers. Account deletion cascades presences; leader/group deletion removes
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
audio {musicPreset: CALM_MUSIC|null, musicPlaying: boolean,
       ambientPreset: RAIN/WHITE_NOISE/CAFE|null, ambientPlaying: boolean}
audioPresets [{id, label, kind: MUSIC/AMBIENT}]
participants [{studentId, name, presence, expiresAt, leader, host, coHost}]
members [{studentId, name}] (active accepted members for role selectors)
```

The browser renders elapsed time from each server snapshot and reconciles on the
server-provided poll interval (default two seconds). Local enable/volume/mute are
not sent to the API. Original synthesized music and ambient layers have independent
shared selection/play state and independent local volume/mute. A null preset is
allowed only when that layer is paused; sending an ambient preset to the music
layer, or vice versa, is 400. Playback requires local consent and failure leaves
the room usable silently. Playback positions are not synchronized. No external
music credentials/assets are required.

## Dated study sessions and calendar (E2 #16)

Session permissions are checked against the current parent relationship on every
list, detail, change, availability and export. Group sessions require an active
student who is an accepted member of the open group; only its leader manages
them. Buddy sessions require the particular active connection and both accounts
active; either participant manages them. Administrators gain no participant access.
Sessions do not expose contacts/emails/tokens or invite unrelated students.

| Method | Path | Input / result |
| --- | --- | --- |
| GET | `/api/sessions/mine` | Eligible sessions for the actor's open groups and active buddy connections; 200 `[SessionDto]` |
| GET | `/api/groups/{groupId}/sessions` | Group agenda; 200 `[SessionDto]` |
| POST | `/api/groups/{groupId}/sessions` | `SessionInput`; leader creates, 201 `SessionDto` |
| GET | `/api/connections/{connectionId}/sessions` | Buddy agenda; 200 `[SessionDto]` |
| POST | `/api/connections/{connectionId}/sessions` | `SessionInput`; participant creates, 201 `SessionDto` |
| GET | `/api/groups/{groupId}/sessions/availability` | Mutual weekly availability; 200 `SessionAvailabilityDto` |
| GET | `/api/connections/{connectionId}/sessions/availability` | Both buddies' mutual weekly availability; 200 `SessionAvailabilityDto` |
| GET | `/api/sessions/{id}` | Current authorized `SessionDto`, 200 |
| PUT | `/api/sessions/{id}` | Whole `SessionInput` plus `expectedVersion`; 200 `SessionDto` |
| POST | `/api/sessions/{id}/cancel` | `{expectedVersion}`; 200 cancelled `SessionDto` |
| GET | `/api/sessions/{id}/calendar` | Authorized `.ics` attachment, `text/calendar; charset=UTF-8`, 200 |

```text
SessionInput {
  title, startLocal, endLocal, timeZone, occurrenceCount,
  agenda?, location?, roomLink?, expectedVersion? // version required for PUT
}
SessionDto {
  id, version, scope: GROUP/BUDDY, groupId|null, connectionId|null,
  title, startLocal, endLocal, startAt, endAt, timeZone, occurrenceCount,
  agenda|null, location|null, roomLink|null, status: SCHEDULED/CANCELLED,
  canManage, occurrences: [{startAt,endAt}], nextOccurrence|null,
  updatedAt, cancelledAt|null
}
SessionAvailabilityDto {
  timeZone, participantCount, sharedSlots: [AvailabilitySlot], sharedMinutes
}
```

`startLocal` and `endLocal` are whole-minute local date-times without an offset;
`timeZone` is a valid IANA zone, such as `Asia/Singapore`. `startAt`, `endAt` and
occurrence timestamps are UTC instants. A series contains one occurrence or a
bounded weekly count (default maximum 52). Every generated start/end preserves
its local wall time and must have exactly one valid offset in that zone: a missing
or repeated DST time anywhere in the series is rejected rather than silently
shifted. Every end must follow its start; default maximum elapsed duration is
24 hours. Bounds come from `app.study-session` configuration.

Title is required and at most 255 characters. Agenda is optional and at most
4000; location is optional and at most 255; room link is optional and at most
2048. Blank optional values become null. A room link is this group's exact
`/groups/{groupId}/room` path, or an HTTPS URL with a host and without user info,
query parameters or fragments; a buddy session cannot use a private group path.
The API does not fetch these URLs. Invalid values return 400 with field errors.

PUT edits the whole saved series. Its non-negative `expectedVersion`, and the
version in cancellation requests, must match the current value; stale writes are
409. A cancelled series cannot be edited. Parent-row locking serializes changes
with connection/group lifecycle operations. Removal immediately revokes group
session access. Ending a connection, closing a group or deactivating an account
revokes the affected access and cancels affected scheduled sessions; reactivation
or a new buddy connection does not restore those schedules. Permanent parent/account
deletion removes dependent records through the documented database relations.

`/sessions/mine` returns scheduled sessions with a current/upcoming occurrence,
ordered by that occurrence's start; cancelled/completed series are absent from Home.
The parent agendas retain authorized session history, including cancelled records.

Frontend presentation selects the current/next occurrence from the authorized
occurrence list using the browser clock. A plan is Ongoing from its start (inclusive)
until its end (exclusive); an ended weekly occurrence advances to the next date.
Current/upcoming group cards can expose the same safe saved room link as details.
Cancelled/completed plans have no shortcut; opening a link does not admit a student
to the room or bypass any backend eligibility/capacity check.

Mutual suggestions intersect the current participants' saved weekly availability
using Team A's `WeeklyAvailability.sharedSlotsWith`; they are campus-local
`Asia/Singapore` blocks, not timezone-converted dated bookings. Missing availability
yields an empty result. Suggestions do not confirm attendance, reserve a time or
prevent manually scheduling another time. The frontend's duration chooser defaults
to 60 positive whole minutes and fits that duration at the beginning of a returned
overlap; insufficient windows are omitted. This chooser is not a new API field:
only selecting a suggestion changes the existing start/end/timezone fields, and
server duration/DST validation remains authoritative. Invalid or no-fit chooser
values do not block an otherwise valid manually entered session. Actual dated sessions feed Home,
group/buddy agendas and recipient-scoped created/updated/cancelled notifications.

Calendar downloads expand the bounded weekly series into individual UTC `VEVENT`
records. Each occurrence keeps a stable series UID plus its index, revision and
timestamps; the export preserves wall time across DST without requiring client
recurrence-rule/timezone interpretation. Escaped text and CRLF/octet line folding
prevent malformed properties. There is no `ATTENDEE`/`ORGANIZER` contact payload,
generated credential or authenticated export API URL. Only validated absolute HTTPS room links are
exported; a private in-app group room path remains available in the authenticated
screen and is omitted from `.ics`. A cancelled session can be downloaded with
cancelled event status while the actor remains eligible for its parent relationship.

The file is a snapshot, with no OAuth, subscription or two-way sync. Clients may
duplicate events or ignore changes/cancellations when re-imported; reducing a
series count does not delete previously imported extra occurrences. Ending the
parent relationship blocks further export and never removes an offline calendar
copy. Client-side whole-series editing is not promised. Real Google Calendar,
Apple Calendar and Outlook import acceptance is recorded separately in
[E2_TESTING.md](E2_TESTING.md), never inferred from parser tests alone.

## Optional campus weather and local themes

`GET /api/weather` uses the existing authenticated student API guard and returns
200 even when the provider is disabled/unavailable. It accepts no user-selected
coordinates or device location.

```text
WeatherDto {
  available, condition: CLEAR/CLOUDY/RAIN/STORM/SNOW/FOG,
  temperatureCelsius|null, isDay, observedAt|null, stale, location
}
```

`available=false` means no usable observation; its default condition/day fields
are fallback metadata and must not be presented as current weather. With usable
cached data after provider failure, `stale=true` labels last-known conditions.
The default backend request targets fixed configured SMU campus coordinates,
uses a four-second timeout, caches success for fifteen minutes, retries failure
after one minute and discards observations older than twenty-four hours. It sends
no student/room/contact/token/schedule data and uses no provider API key. The test
profile disables provider requests.

The room's original CSS scene offers automatic live campus conditions or locally
selected clear/cloudy/rain/storm/snow/fog. Manual choices are decorative and saved
only in that browser. Without usable live data, the scene follows Singapore day/night
and the manual choices remain available. `prefers-reduced-motion` stops animations.
Weather data attribution links to [Open-Meteo](https://open-meteo.com/) (CC BY 4.0);
provider outages do not change shared timer/audio state or block a room/session action.

## Notifications

| Method | Path | Request | Response | Auth |
| --- | --- | --- | --- | --- |
| GET | `/api/notifications?filter=ALL` | `ALL`, `REQUESTS`, `GROUPS`; defaults ALL | 200 `NotificationDto[]`, newest first | Student |
| GET | `/api/notifications/unread-count` | none | 200 `{count:number}` | Student |
| POST | `/api/notifications/{id}/read` | none | 200 notification with `read:true` | Recipient |
| POST | `/api/notifications/read-all` | none | 204 | Student |

Notification DTO: `{id,type,message,read,createdAt,resourceType,resourceId,eventKey,requestDirection}`.
`resourceType` is `MATCH_REQUEST`, `GROUP`, `STUDENT`, `SESSION` or null. Resource ID and type are both null
for safe generic deletion notices and older notices without metadata. `eventKey` is a nullable
stable event identity, never an authentication token. MATCH_REQUEST links to requests/
connections; GROUP links to the group; STUDENT links to its public/connected profile;
SESSION links to its authorized session detail. Opening
a target still uses its authorized endpoint. A now unavailable target returns a safe 404;
notification metadata does not confer permissions.

E2 adds `GROUP_SESSION_CREATED/UPDATED/CANCELLED` and
`BUDDY_SESSION_CREATED/UPDATED/CANCELLED`. GROUPS includes group session notices;
REQUESTS includes buddy session notices; ALL includes both. Recipients are current
active members/participants other than the initiating actor; lifecycle cancellation
notices use the same recipient boundary. Session revision/change/recipient identities
deduplicate events within successful transactions. No reminder scheduler is implied.

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
| Request withdrawn by its sender, or cancelled by deactivation or deletion | Other participant, one | `MATCH_REQUEST_CANCELLED` |
| Disconnect | Other participant, one | `CONNECTION_ENDED` |
| Group application | Leader, one | `GROUP_JOIN_REQUEST_RECEIVED` |
| Group accept / reject | Applicant, one | `GROUP_JOIN_REQUEST_ACCEPTED` / `GROUP_JOIN_REQUEST_REJECTED` |
| Application withdrawal | Leader, one | `GROUP_JOIN_REQUEST_WITHDRAWN` |
| Member removal | Removed member, one | `GROUP_MEMBER_REMOVED` |
| Member leaves | Leader, one | `GROUP_MEMBER_LEFT` |
| Group closure | Each pending applicant, one rejection; each accepted member other than leader, one closure | `GROUP_JOIN_REQUEST_REJECTED`, `GROUP_CLOSED` |
| Student deactivation | Each active buddy and pending buddy counterpart; led-group closure recipients; leaders of withdrawn applications and removed memberships | Corresponding ended/cancelled/rejected/withdrawn/removed events; the ended-connection notice has no profile link |
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
applicants, withdraws the student's pending applications and removes their memberships in open
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

Relationship, profile and room/session transition writes use the shared PostgreSQL
transaction advisory lock `4422026`. Account lifecycle and opt-in seed use its exclusive
variant, so cleanup cannot race with admission of a new relationship in another process.
Affected user rows are locked in ascending ID order before group and request/connection
rows, with authorization/state rechecked after waiting. Group capacity edits, removal,
closure and approval share the group lock. Room state reads and existing-lease heartbeat
renewals deliberately do not take that lock; they recheck access, and renewal cannot
create a deleted or expired lease. The protocol does not claim that every read or minor
update takes every lock. No JVM-only lock replaces the cross-instance transition protocol.

Login locks the normalized email lookup before loading the account. If an administrator's
email edit commits while that lookup waits, the old email is rejected and cannot overwrite the
edited identity when recording a successful login.

Partial unique indexes protect unordered pending buddy pairs, unordered active connection
pairs, pending `(study_group_id,student_id)` applications and `(recipient_id,event_key)` notices.
Membership `(study_group_id,student_id)` stays unique. Answered/ended history remains legal.
The migrations create these constraints; Hibernate `update` does not. RLS and browser-role privilege removal
protect the alternate Supabase access path; operational evidence is recorded separately.

## Matching configuration (Team A)

All three configuration operations require an active administrator. Students receive 403;
missing/invalid authentication receives 401. They return DTOs, never persistence entities.

| Method | Path | Request | Response |
| --- | --- | --- | --- |
| GET | `/api/admin/matching-config` | none | 200 `MatchingConfigDto` |
| PUT | `/api/admin/matching-config` | Full replacement and required `expectedVersion` | 200 saved `MatchingConfigDto` |
| POST | `/api/admin/matching-config/restore-defaults` | `{expectedVersion:number}` | 200 default `MatchingConfigDto` with increased version |

Example default read:

```json
{
  "activeStrategy": "BALANCED",
  "weights": {
    "BALANCED": { "COURSE": 0.3, "AVAILABILITY": 0.3, "STUDY_MODE": 0.2, "STUDY_GOAL": 0.1, "GROUP_SIZE": 0.1 },
    "AVAILABILITY_FIRST": { "COURSE": 0.25, "AVAILABILITY": 0.4, "STUDY_MODE": 0.15, "STUDY_GOAL": 0.1, "GROUP_SIZE": 0.1 },
    "COURSE_FIRST": { "COURSE": 0.4, "AVAILABILITY": 0.25, "STUDY_MODE": 0.15, "STUDY_GOAL": 0.1, "GROUP_SIZE": 0.1 }
  },
  "enabledCriteria": ["COURSE", "AVAILABILITY", "STUDY_MODE", "STUDY_GOAL", "GROUP_SIZE"],
  "minScore": 0,
  "version": 0,
  "usingDefaults": true
}
```

The PUT body replaces `version`/`usingDefaults` with `expectedVersion` and includes every
other field shown. `activeStrategy` is `BALANCED`, `AVAILABILITY_FIRST` or `COURSE_FIRST`.
All three weight maps must include all five criteria. Every weight is finite and between 0
and 1; disabled criteria require 0. Enabled weights total 1 within `0.001` for decimal
rounding in every strategy. At least one criterion must be enabled. `minScore` is finite
and between 0 and 1, inclusive. Missing required top-level fields produce 400
`VALIDATION_FAILED`; unknown enums and invalid service-level weights, totals, enabled sets
or thresholds produce 400 `INVALID_INPUT`. Neither changes any saved setting. Valid supplied
fractional weights are not rounded on
save/read/scoring. Existing invalid-total legacy weights are rescaled only on read.

`expectedVersion` is required, non-negative, and must equal the current persistent version.
Concurrent or stale save/restore returns 409 `STALE_MATCHING_CONFIG`; the caller reloads before deciding whether to
replace the newer settings. Each successful save/restore increments the revision even when
the chosen values are equal. Until saved overrides exist, `app.matching.default-*` values
apply and `usingDefaults` is true. Saving sets it false. Restore explicitly clears overrides,
returns current external defaults, sets it true and preserves the increasing version. Saved
overrides and version survive application restart; restart does not silently restore defaults.

Configuration GET and match search read a repeatable database snapshot. A concurrent save
may become visible on the next request; it cannot mix a new strategy, enabled set or threshold
with previous weights/version inside one response. Configuration mutation is serialized in
PostgreSQL, including the first save when no prior override rows exist.

Balanced ranks by the total. Availability-first ranks by bands of unique shared hours first
(`app.matching.priority-tiers.availability-hours`, default 3 h or more / 1 to 3 h / under 1 h),
then by total within a band; Course-first ranks by bands of course score first, then total.
Course bands are the scorer's own outcomes, 1 and `app.matching.scoring.takes-course-score`
(with a course in focus: wants a buddy for it / takes it / neither; with no course in focus:
all courses shared / at least that share shared / fewer), so there is no separate course-band
setting. A candidate whose priority criterion is unknown always ranks in the lowest band,
whatever `unknown-score` is. Bands stop a small difference in the priority criterion from outranking
every other criterion. Each strategy's default weights give its priority criterion the largest
share, so the displayed total and quality label reflect what the list is ranked by; the weight
stays editable. If the priority is disabled, its raw score is ignored and the strategy
falls back to total. Disabled criteria never influence the strategy comparison through either route. Explicit
student filters and alternative sorts remain available: disabling availability scoring does
not remove a minimum-shared-hours filter or Most shared hours sort; disabling course scoring
does not remove the required course-or-goal discovery context.

The admin screen loads these fields, separates unsaved edits from the saved revision,
validates near each input, confirms restore, and reloads the returned saved state. The same
version is exposed to matching and E1 consumers. E1's card identity covers configuration
and scoring inputs; its separate anonymous evidence cache may reuse equivalent wording only
after the current card identity has been checked (see the E1 contract below).

## Match search (Team A)

Student only. `GET /api/matches` requires a current `courseId` or `studyGoal` (or both).
It excludes self, inactive/deleted accounts and existing active connections. Incoming and
outgoing pending candidates remain visible with disabled duplicate-send actions. The
Find buddies page (`/find-buddies`) and Home preview consume this same response.

| Query parameter | Values | Meaning |
| --- | --- | --- |
| `courseId` | positive known course ID | Candidate targets or takes this course; overrides caller's saved target for course scoring |
| `studyGoal` | `CONCEPT_REVIEW`, `PROBLEM_SOLVING`, `EXAM_PREPARATION`, `PROJECT_DISCUSSION` | Candidate has this goal; added to the caller's saved goals for goal scoring |
| `studyMode` | `IN_PERSON`, `ONLINE`, `EITHER`, optional | Same mode or candidate `EITHER`; `EITHER` filters nothing; missing candidate mode cannot satisfy a specific mode |
| `groupSize` | `ONE_TO_ONE`, `SMALL_GROUP`, `EITHER`, optional | Candidate range overlaps 2-2 or 3-configured maximum; `EITHER` filters nothing; missing range cannot satisfy a specific category |
| `minSharedHours` | positive integer, optional | At least this many unique shared hours from saved weekly availability, counting only shared blocks of at least `app.matching.scoring.min-session-minutes` |
| `minQuality` | `FAIR`, `GOOD`, `STRONG`, optional | Hide lower quality; `FAIR` imposes no additional minimum |
| `sort` | `BEST_MATCH`, `MOST_SHARED_HOURS`, `SAME_YEAR_FIRST`, optional | Defaults `BEST_MATCH`; alternative sorts use shared time or equality to caller's year first |
| `limit` | positive integer, optional | Defaults `app.matching.default-result-limit` (20), cannot exceed `app.matching.max-result-limit` (50) |

All supplied filters combine with AND. Personal filters never change global settings.
The global admin minimum total score and the optional minimum-quality filter both apply.
Unknown course returns 404. Missing both course/goal, non-positive IDs/limits/hours, too large
limit and unknown enum inputs return 400. Empty results are 200 with `matches:[]` and
`totalMatches:0`; they are not an API failure.

The full eligible filtered set is sorted before limiting. `totalMatches` counts that set
before the limit; this endpoint is bounded top-results discovery, not offset pagination.
`BEST_MATCH` uses active strategy, then more shared hours, then case-insensitive name, then
student ID. Alternative sorts apply descending unique shared minutes or same-year equality
first, then the same strategy/shared-hours/name/ID chain. This makes duplicate names/tied scores deterministic. The frontend
retains server order; quality badges do not regroup the cards.

Response fields:

```text
MatchSearchResultDto {
  strategy: BALANCED | AVAILABILITY_FIRST | COURSE_FIRST,
  configurationVersion: non-negative integer,
  sort: BEST_MATCH | MOST_SHARED_HOURS | SAME_YEAR_FIRST,
  limit: positive integer,
  totalMatches: non-negative integer,
  matches: MatchResultDto[]
}
MatchResultDto {
  studentId, name, school, programme, yearOfStudy,
  studyMode: IN_PERSON | ONLINE | EITHER | null,
  studyGoals: StudyGoal[],
  relevantCourse: {id,code,name} | null,
  totalScore: number in [0,1],
  explanation: deterministic string from the configured MatchExplainer,
  evaluationId: opaque lowercase SHA-256 string for this card evaluation,
  quality: STRONG | GOOD | FAIR,
  breakdown: {criterion,score:number in [0,1],enabled:boolean,known:boolean}[],
  sharedHoursPerWeek: number,
  sharedSlots: {dayOfWeek,startTime,endTime}[],
  relationship: STRANGER | INCOMING_PENDING | OUTGOING_PENDING
}
```

The response's strategy and configuration version belong to the same scoring snapshot.
`totalScore` and criterion scores retain calculation precision; only display percentages
are rounded. Percentages mean preference compatibility, never a success probability.
Breakdown has all five criteria in declaration order; disabled entries have `enabled:false`
and score 0. An enabled preference that either student left empty has `known:false` and
scores `app.matching.scoring.unknown-score` (0 by default); clients show it as not shared
rather than as partial agreement. Availability and saved-goal scores are symmetric: A's
score against B equals B's against A. Course scoring and a searched goal follow the
searcher's current course or goal, so those can differ by direction. Quality uses the configured Strong/Good cut-offs on the total, with
Fair below Good. The deterministic explanation describes enabled criterion compatibility
(unknown criteria as "not shared"), unique shared hours and quality; it uses no AI or
fabricated match. `MatchExplainer` receives only the score, never a profile, so a future
explainer cannot reveal private fields.

`relevantCourse` is the current search course if supplied. For a goal-only search it is the
first common target/enrolled course ordered by course code then ID, or null when there is
none. Current search intent, saved buddy target and courses taken are distinct; a search
need not be an enrolled course to be valid.

Weekly slots are campus-local SGT and Monday-first. Duplicate/overlapping/adjacent inputs
are merged per day before intersection. Touching intervals share no minutes. The same unique
minutes drive availability scoring, filters, sorting, shared hours and heatmap. Shared hours
are derived from minutes rather than repeated pairwise overlaps.

Discovery results never include contact number, email, password/token fields or raw entities.
Profile navigation and optional-message requests use the existing Team C APIs. An existing
connection is absent from discovery and remains available on Connections. The page only
shows Send match request for `STRANGER`. The request service still checks eligibility on the
POST, so a concurrent send/accept cannot bypass duplicate/connection rules. After a successful
POST the card immediately shows outgoing pending while refreshing. Its post-send refresh or
a later response supersedes that acknowledgement, including a decline/cancellation; a
response started before confirmation cannot clear it.

Both matching views handle first use/incomplete profile, loading, no eligible results,
error/retry, filters/reset and requests without exposing private data. Home selects a saved
target course or goal to form a valid search; if neither exists it links to preference setup
instead of making an unqualified discovery call. It displays the same active strategy,
configuration revision and result data as Find buddies. Home sends no hard-coded `limit=3`: it
uses the configured default result limit and displays up to three students from that response.

## Match explanation (E1)

Student only. `GET /api/matches/{candidateId}/explanation` is fetched when the student clicks
**Explain this match**. Send the card's required `evaluationId` and the same `courseId`,
`studyGoal`, `studyMode`, `groupSize`, `minSharedHours` and `minQuality` values that produced
it. `sort` and `limit` affect discovery only and are omitted.

```json
{
  "candidateId": 42,
  "evaluationId": "7c88b58d646046a112c832edfc6ea2d93c81715bef3fbe92e1aef7768cc01849",
  "text": "Your course preferences align, with about 2 hours per week in common.",
  "source": "AI"
}
```

`source` is `AI` for validated Gemini wording and `STANDARD` for the matching engine's own
explanation. Both carry the matching evaluation ID. The frontend renders text literally,
labels AI as potentially inaccurate and keeps numeric scores independent of wording.

### Card freshness and lifecycle

Search and direct evaluation compute the same deterministic fingerprint from explicit,
ordered inputs: both students' IDs, catalogue courses/target, goals, study mode, group-size
ranges and normalized weekly availability; the six search fields above; strategy, saved
configuration revision, minimum score and ordered criterion weights/enabled flags; scoring
rules; and exact total, quality, criterion scores/known flags and shared hours. Only the
opaque digest goes to the browser; the fingerprint input is never provider evidence.
Relationship state, names, contact details, sort and result limit are not fingerprint inputs.

Before cache, quota or provider work, the service recomputes the evaluation server-side.
A mismatched fingerprint returns **409 `STALE_MATCH_EVALUATION`**, with no explanation text.
The UI offers **Refresh matches**, fetches current cards, and lets the student request their
explanation again. A completed or in-flight panel belongs to its candidate and evaluation ID;
a different ID clears its text and aborts/ignores the old response, including a nonpurging
post-request refresh. A relationship-only update retains the answer when scoring is unchanged.
No client-supplied scores or persisted search-session records are used.

The fingerprint is a freshness check, not an authorization token or discovery-membership
proof. Direct evaluation retains its existing contract: authenticated active student,
valid search and active non-self candidate. Discovery filters, minimum score/quality and
connected-candidate exclusion do not independently restrict this direct operation.

| Status | Meaning |
| --- | --- |
| 400 | Invalid search, or missing/malformed evaluation ID (64 lowercase hexadecimal characters required) |
| 401 / 403 | Signed out / not an eligible student actor |
| 404 | Unknown course, or unknown, inactive, deleted or self candidate |
| 409 `STALE_MATCH_EVALUATION` | Scoring inputs changed since the card was issued; refresh matches |

### Provider evidence and cache policy

Only `ExplanationEvidence` goes to Gemini: rounded total/criterion scores, quality, strategy,
enabled/known flags, rounded weekly shared hours, explicit scoring/shared course code and
name, searched goal/shared effective goals, study modes and categorical group sizes. Never
names, student/evaluation IDs, contact details, school, year, messages or schedules.

- `scoringCourse` is the searched course, otherwise the caller's saved target. Null means
  course scoring compares course sets; a non-null focus does not imply the candidate takes it.
- `sharedCourse` is the first course actually listed as targeted/taken by both profiles,
  ordered by code then ID. It may differ from the scoring focus and cannot explain that
  criterion's score by itself. The card's existing `relevantCourse` display contract is unchanged.
- `searchedGoal` records current intent. `sharedGoals` intersects the candidate's saved goals
  with the caller's saved goals **plus** that searched goal, using the scorer's shared helper.

The semantic cache key is SHA-256 of evidence, prompt version (`v5`) and model. Scores are
rounded to hundredths and hours to half-hours; the prompt describes hours as approximate.
Equivalent anonymous evidence may reuse wording across students or exact card evaluations.
Changed raw inputs still require a fresh card, even when rounded evidence stays equivalent.
Changed evidence, prompt version or model uses another cache row. No student IDs are stored.

Fresh output and cached text use the same current length/content policy: reject malformed
JSON, obvious success/percentage claims, unsupported code-shaped course references and numeric
literals other than the supplied weekly hours. These bounded checks do not verify every
natural-language assertion. Rejected cached text returns standard wording without consuming
quota or calling Gemini; it is not served merely because an earlier policy accepted it.

Disabled/unconfigured AI, exhausted quota, provider failure/timeout, unusable output and
cache-read `DataAccessException` return standard wording. A cache-write `DataAccessException`
returns the already validated AI answer. Optional cache logs contain only operation and
exception class. Authentication, matching, unavailable-candidate and core database failures
retain their normal error behavior. The provider wait holds no matching database transaction.

Quota is a per-process daily budget, reset on restart; it is not a durable multi-instance
billing cap. Concurrent cache misses may each call the provider; insert-on-conflict prevents
duplicate cache rows only. See [E1 testing](E1_TESTING.md) for evidence and remaining acceptance.

## Study plans (AI study assistant)

Proposed bonus extension on `feat/team-b/study-roadmap`; not part of the agreed selected extras
until the team accepts it. Student only. This section covers personal plans; session plans for
a group or buddy session are added with their own endpoints.

| Method | Path | Request | Response | Who |
| --- | --- | --- | --- | --- |
| POST | `/api/study-plans` | `multipart/form-data`: `PersonalPlanInput` fields plus 0 to `max-files` parts named `files` | 201 `StudyPlanDto` | Student |
| GET | `/api/study-plans` | none | 200 `StudyPlanSummaryDto[]`, own personal plans newest first | Student |
| GET | `/api/study-plans/{planId}` | none | 200 `StudyPlanDto` | Owner |
| PUT | `/api/study-plans/{planId}/entries/{entryId}/progress` | `{ "completed": true }` | 200 `StudyPlanDto` | Owner |
| DELETE | `/api/study-plans/{planId}` | none | 204 | Owner |

```text
PersonalPlanInput   (multipart form fields)
  courseId, hoursPerWeek (1 to max-hours-per-week), examDate? (yyyy-MM-dd), studyGoal?, topicsText?
StudyPlanDto
  { id, scope: PERSONAL/SESSION, course: CourseDto, sessionId|null, firstBlockDate, blockCount,
    minutesPerBlock, examDate|null, studyGoal|null, sourceKind: UPLOAD/PASTE, sourceFilenames[],
    sourceTopics[], canManage, createdAt, updatedAt,
    entries: [{ id, sequence, scheduledDate, title, plannedMinutes, tasks[], completed }] }
StudyPlanSummaryDto
  { id, scope, course, firstBlockDate, blockCount, examDate|null, studyGoal|null,
    entryCount, completedCount, createdAt }
```

A plan starts today in `Asia/Singapore` (`study-plan.time-zone`). Without `examDate` it covers
this week alone; with one it covers every week up to the exam, so an exam 1 to 7 days away is one
week and 8 to 14 days two. The exam must be after today and within `max-weeks` weeks. Each
week holds `hoursPerWeek` hours. Week *n* of a plan starts on `firstBlockDate` plus 7 × (*n* − 1)
days, which is each entry's `scheduledDate`.

The material is one or more `files` (PDF, `.txt` or `.md`) or `topicsText`, never both. Files
together must fit `max-upload-size` and `max-text-file-characters`, and each file's contents must
match its extension. Files are sent to Google Gemini for this one request and never stored; only
their names are kept. The roadmap Gemini returns must pass the checks in the README's
"AI study assistant" configuration section; a student may keep several plans for one course.

Errors, in the order they are checked, so a bad request never spends the student's AI allowance:

| Status | Code | When |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | A missing or malformed field; an unknown `courseId`; `hoursPerWeek` out of range; an exam today, in the past or beyond `max-weeks`; no material, or both files and topics; too many files; an empty file; topics too long |
| 413 | `FILE_TOO_LARGE` | Files together over `max-upload-size`, or text files over `max-text-file-characters` |
| 415 | `UNSUPPORTED_FILE_TYPE` | A file other than PDF, `.txt` or `.md`, or whose contents do not match its extension |
| 503 | `AI_UNAVAILABLE` | The assistant is switched off, over its daily allowance, rate limited or timed out, or Gemini's reply failed the checks. Nothing is saved |
| 404 | `NOT_FOUND` | Reading, ticking or deleting a plan that does not exist or is not the student's own, or an entry not in that plan |

The plan is created in three steps: a short read that checks the request, the Gemini call, and
a short write that checks the student is still active before saving. No database transaction or
account lock is held while Gemini answers.

## UI use of the existing contract

The student home page (`/home`, Team B) reads existing endpoints. Team A's matching preview
uses the documented `/api/matches` response alongside the other sections. It reads these
endpoints, each section independently so one failure does not hide the others:
`GET /api/profile/me` (profile progress and weekly availability), `GET /api/students/me/summary` (activity counts),
`GET /api/match-requests/incoming` (pending requests, preview only; accept/decline stays on
Connections), `GET /api/groups/mine` (open groups), `GET /api/notifications` (latest five)
and E2 `GET /api/sessions/mine` (actual upcoming dated group/buddy sessions).
A change to any of these response shapes needs a matching change to the home page.

The original baseline UI polish added no endpoint or DTO field. E2 now adds the
room/session/weather contracts documented above. Group capacity/leader/member/application
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

E2 student routes are `/groups/{id}/room`, `/sessions`, `/sessions/{id}`,
`/sessions/{id}/edit`, `/groups/{id}/sessions` and `/connections/{id}/sessions`;
each parent list has a `/new` editor for its authorized managers. Group detail
loads a dated-session agenda only for accepted members of an open group, while
Connections offers a session link for each active buddy. Session notification
links target `/sessions/{id}` and recheck permission through the detail endpoint.
Local browser drafts, room host roles and notification metadata never grant
session management or calendar access.

### AchievementsDto

`GET /api/achievements/me` returns the authenticated student's private current standing. It is
read-only and recomputed from existing records; there is no award table or permanent badge history.
Anonymous requests receive 401 and administrators receive 403. There is no achievement endpoint
for another student, and public, connected and administrator profile payloads never embed this data.

```
AchievementsDto { level, maxLevel, points, maxPoints, pointsToNextLevel, pointsPerLevel, earnedCount, totalCount,
                  streak: StreakDto, badges: BadgeDto[] }
StreakDto       { currentWeeks, bestWeeks, currentRunWeeks: LocalDate[], weekZone }   // Mondays, oldest first
BadgeDto        { key, title, description, tier: BRONZE|SILVER|GOLD|null, nextTier: BRONZE|SILVER|GOLD|null,
                  current, target, earned, earnedHint }
```

| Badge key | Count and lifecycle |
| --- | --- |
| `CONNECTOR` | Active accepted connections; ending one lowers the count |
| `FIRST_MOVE` | Retained sent match requests in any status, including declined/cancelled/accepted; disconnecting does not erase those requests |
| `TEAM_PLAYER` | Current memberships in open groups the student does not lead; leaving, removal or closure lowers the count |
| `FOUNDER` | Retained groups created by the student, including closed groups |
| `PLANNER` | Individual non-cancelled scheduled occurrences on currently accessible open-group/active-buddy schedules, including past and future occurrences, from the start of the current membership or exact connection onward |
| `GROVE_KEEPER` | Longest consecutive planned-week run derived from those currently eligible occurrences; it can decrease if the source schedules change or become inaccessible |

The schedule read uses one projection query including each membership's `joinedAt` or connection's
`createdAt`. A newcomer or readmitted member does not inherit occurrences before that current tenure.
Inactive buddies, ended connections, closed groups, removed memberships and cancelled sessions are
ineligible. Each weekly recurrence counts separately for Planner, even when its date is in a future week.

For the streak, weeks start on Monday in `app.achievements.week-zone`, returned as `weekZone`.
Each distinct eligible week counts once. Future weeks are excluded, but an occurrence later in the
current week counts immediately because the metric measures planning. A run ending last week remains
current while this week is still open; a gap of a whole week resets the current run. `bestWeeks` is the
longest run in currently eligible data, not a stored all-time record. Removing/cancelling/editing a
schedule can change both streak values. Neither scheduling nor the room timer records attendance,
completed focus time or study quality.

Thresholds are positive and nondecreasing bronze/silver/gold values from `app.achievements.*`;
`points-per-level` must be positive. `earned` means a badge currently has a tier. Only the highest tier
contributes points (Bronze 1, Silver 2, Gold 3); tiers are not added together. `target` is the next
tier's threshold, or the Gold threshold with `nextTier=null` at Gold. Counts may exceed a Gold target.
Six Gold badges yield `maxPoints=18`; the default maximum is level 7. The last level step is shortened
when needed so every configured maximum is reachable. At maximum standing `pointsToNextLevel=0`.
Tiers, points and level can fall when eligible records decrease; retained request/group history only
disappears when its underlying records do.

Badge actions are a frontend allowlist keyed by the fixed badge catalogue; the API never supplies
arbitrary navigation URLs. The own-profile summary fetches `/me` only after the signed-in student's
role, route id, profile id and `SELF` relationship agree. It unmounts/aborts on owner or profile changes.
