# Team C implementation diagrams

These diagrams describe the implemented composition/DTO/transaction model. The earlier
discussion's Student Leader inheritance sketch is not the implementation: `Student`
has a one-to-one `User` association with a shared `@MapsId` identity, and
`StudyGroup.leader` refers to an ordinary student.
`Role` contains only STUDENT and ADMIN. Matching strategies/scorers remain Team A scope.
The [API contract](../API_CONTRACT.md) contains full DTO fields/endpoints and the
[decisions](../DESIGN_DECISIONS.md) explain privacy, lifecycle and locking choices.
Use the [diagram index](README.md) for the controlled SVG exports and review status.

## Use cases and permissions

```mermaid
flowchart LR
    Student[Active student] --> Buddy[Send / list / accept / decline buddy request]
    Student --> End[End own buddy connection]
    Student --> Profile[Read study profile]
    Student --> Browse[Browse / filter / create groups]
    Student --> Apply[Apply to group / read own applications]
    Student --> Events[Read / filter notifications and mark read]
    Student --> Leader[Student who leads this group]
    Leader --> Manage[Edit / approve / reject / remove member / close]
    Admin[Active administrator] --> Accounts[Create / read / edit user accounts]
    Admin --> Lifecycle[Deactivate / reactivate / delete permanently]
    Profile --> Privacy[Contact only for self or active buddy]
    Lifecycle --> Guard[Block removing your own account]
```

An incoming request can be answered only by its receiver; a connection can be ended only
by a participant. Leader authority is checked against the URL's particular group. Admin
authority does not bypass profile contact privacy. Server checks remain necessary when
the UI hides or disables unavailable actions.

## E2 room and session boundaries

```mermaid
classDiagram
    StudyGroup "1" --> "0..1" StudyRoom
    StudyRoom --> PomodoroTimer : reconstructs immutable value
    StudyRoom "1" --> "0..*" RoomPresence
    RoomPresence --> Student : authenticated owner
    StudyRoom --> Student : optional host and coHost
    StudyRoomController --> StudyRoomService
    StudyRoomService --> RoomAccess
    StudyRoomService --> RoomAssembler
    StudyRoomService --> StudyRoomRepository
    StudyRoomService --> RoomPresenceRepository
    RoomAccess --> AccountAccess
    RoomAccess --> StudyGroupLookup
    RoomAccess --> GroupMembershipRepository
    RoomAssembler --> StudyRoomDto
    StudyGroupService --> RoomLifecycle : revoke member leases and roles
    GroupClosure --> RoomLifecycle : close room
    StudentDeactivation --> RoomLifecycle : revoke account leases and roles
    RoomLifecycle --> RoomPresenceRepository
    RoomLifecycle --> StudyRoomRepository
    StudyGroup "0..1" --> "0..*" StudySession
    Connection "0..1" --> "0..*" StudySession
    StudySession --> SessionSchedule : immutable local-time series
    StudySessionController --> StudySessionService
    StudySessionService --> SessionAccess
    StudySessionService --> SessionAssembler
    StudySessionService --> SessionAvailability
    StudySessionService --> SessionCalendar
    StudySessionService --> SessionEvents
    StudySessionService --> StudySessionRepository
    SessionAssembler --> SessionDto
    SessionAvailability --> WeeklyAvailability : reuse Team A intersection
    SessionEvents --> NotificationService
    StudySessionLifecycle --> StudySessionRepository
    StudySessionLifecycle --> SessionEvents
    class SessionSchedule {
        LocalDateTime startLocal
        LocalDateTime endLocal
        String timeZone
        int occurrenceCount
        occurrences()
    }
    class PomodoroTimer {
        Duration focusDuration
        Duration breakDuration
        TimerPhase phase
        TimerStatus status
        Duration remainingAtAnchor
        Instant anchor
        snapshot(now)
        start(now)
        pause(now)
        resume(now)
        reset()
    }
```

Room controls persist a versioned timer snapshot; presence heartbeats update
separate expiring leases. State reads use `RoomAccess.readMember` without row locks.
Heartbeats use that same authorization check and renew only an existing unexpired
lease; concurrent deletion fails renewal. Join/leave and timer/audio/configuration
changes use `lockMember`: account locks precede the group lock, followed by a current
membership check. Assemblers expose only DTOs. The leader retains controls, delegated
hosts must be present, and distinct students count toward capacity. Membership/account/
closure operations revoke leases and roles transactionally, so readmission needs a fresh join.

Each dated session belongs to exactly one group or connection. Group leaders and
the relevant buddy pair manage their own series; current parent eligibility gates
detail, suggestions and calendar export. `SessionSchedule` generates bounded UTC
occurrences from local civil time and an IANA zone; missing/ambiguous DST endpoints
are rejected. Assemblers expose no private contact/email/token fields. E1 matching
explanations remain separate work.

## Session changes, events and calendar export

```mermaid
sequenceDiagram
    actor Student
    participant UI as Session screen
    participant API as StudySessionController
    participant Service as StudySessionService
    participant Access as SessionAccess
    participant Schedule as SessionSchedule
    participant Events as SessionEvents
    participant Calendar as SessionCalendar
    participant DB as PostgreSQL
    Student->>UI: save whole group or buddy series
    UI->>API: input and expectedVersion plus JWT
    API->>Service: validated input and principal ID
    Service->>Access: lock current parent and verify manager
    Access->>DB: accounts before group or connection
    Service->>Schedule: validate zone and each weekly start/end
    Schedule-->>Service: bounded UTC occurrences
    Service->>DB: save observed-version change
    Service->>Events: notify current active recipients
    Events->>DB: revision/change/recipient event keys
    Service-->>UI: SessionDto after successful transaction
    Student->>UI: download calendar
    UI->>API: authenticated export request
    API->>Service: calendar(sessionId, principal ID)
    Service->>Access: recheck current parent eligibility
    Service->>Calendar: export authorized session
    Calendar-->>UI: private single/bounded UTC event snapshot
    Note over UI,Calendar: Stable occurrence UIDs; no contact payload or automatic client sync
    Note over Service,DB: Notification failure rolls back the saved session change
```

Parent closure/connection end/deactivation cancels affected future schedules and
revokes access; a notification link does not restore authorization. Cancellation
files are available only while the parent remains accessible. Offline imported
events are independent copies, so shortening/cancellation/revocation cannot erase
them. Re-export/import limitations and real-client acceptance are in
[E2_TESTING.md](../E2_TESTING.md).

## Lifecycles (state machines)

Each lifecycle is owned by its entity. `MatchRequest`, `GroupJoinRequest`, `Connection` and
`StudyGroup` expose intention-revealing transition methods (`accept()`, `decline()`,
`cancel()`, `reject()`, `withdraw()`, `end()`, `close()`) and no status setter, so an illegal
transition cannot be written from outside the class. Every transition out of a final state
throws, and the API reports it as 409.

### Match request and connection

```mermaid
stateDiagram-v2
    direction LR
    [*] --> PENDING : sender sends request<br/>(optional message)
    PENDING --> ACCEPTED : receiver accepts
    PENDING --> DECLINED : receiver declines<br/>(after confirming)
    PENDING --> CANCELLED : sender withdraws it, or<br/>either account deactivated
    ACCEPTED --> [*]
    DECLINED --> [*]
    CANCELLED --> [*]

    state ACCEPTED {
        direction LR
        [*] --> Active : Connection created<br/>(contact number visible)
        Active --> Ended : a participant ends it, or<br/>an account is deactivated
        Ended --> [*] : contact hidden again
    }
```

`MatchRequestService.send` rejects these before a request is created:

- **409:** sending to yourself, an active connection already exists between the pair, or a
  pending request already exists in *either* direction.
- **404:** the receiver is inactive or unknown.

Only the receiver may accept or decline, and only the sender may withdraw (cancel); anyone
else gets 403. The other student is notified of a decline or a withdrawal. Accepting and
creating the `Connection` happen in one transaction with the request row locked, so a double
accept, or an accept racing a decline or withdrawal, resolves to a single outcome.

An ended connection keeps `endedAt` for history. Contact privacy is recomputed on every read,
so the contact number disappears as soon as the connection ends, and the pair may send a new
request afterwards. A withdrawn request likewise frees the pair to send a new one.

### Group join request and membership

```mermaid
stateDiagram-v2
    direction LR
    [*] --> PENDING : student applies<br/>(group active, not already<br/>member or pending)
    PENDING --> ACCEPTED : leader approves<br/>(group not full)
    PENDING --> REJECTED : leader rejects (after<br/>confirming), or group closed
    PENDING --> WITHDRAWN : applicant withdraws, or<br/>applicant deactivated
    ACCEPTED --> [*] : GroupMembership created
    REJECTED --> [*]
    WITHDRAWN --> [*]
```

```mermaid
stateDiagram-v2
    direction LR
    [*] --> Member : application approved
    Member --> [*] : member leaves (after confirming),<br/>leader removes them, or<br/>member deactivated (open<br/>groups only; closed groups<br/>keep members as history)
```

```mermaid
stateDiagram-v2
    direction LR
    [*] --> Open : leader creates group<br/>(leader becomes first member)
    Open --> Open : leader edits details<br/>(max size ≥ current members)
    Open --> Closed : leader closes, or<br/>leader account deactivated
    Closed --> [*] : pending applications rejected,<br/>members notified
```

Approval is serialised on the group row, so two approvals cannot both take the last place.
Leaving or being removed deletes the `GroupMembership` row, and the leader is notified when a
member leaves or an applicant withdraws. A leader cannot leave their own group; they close it
instead. A closed group rejects edits, approvals, rejections, removals and leaving.

## Domain and OO boundaries

```mermaid
classDiagram
    class User {
        Long id
        String email
        String passwordHash
        Role role
        boolean active
        long tokenVersion
        Instant lastLoginAt
        deactivate()
        setActive(boolean)
        recordLogin()
    }
    class Student {
        Long id
        User user
        String name
        String contactNumber
    }
    class MatchRequest {
        MatchRequestStatus status
        MatchRequestOrigin origin
        Instant createdAt
        Instant respondedAt
        accept()
        decline()
        cancel()
    }
    class Connection {
        Instant endedAt
        end()
        isActive()
        otherStudent()
    }
    class StudyGroup {
        String name
        int maxGroupSize
        boolean active
        close()
        isLeader()
    }
    class GroupJoinRequest {
        GroupJoinRequestStatus status
        accept()
        reject()
        withdraw()
    }
    class GroupMembership {
        Instant joinedAt
    }
    class Notification {
        NotificationType type
        String eventKey
        boolean read
        markRead()
    }
    class ProfileDto {
        <<interface>>
    }
    class PublicProfileDto {
        public study fields
        ProfileRelationshipDto relationship
    }
    class ConnectedProfileDto {
        public study fields
        String contactNumber
        ProfileRelationshipDto relationship
    }
    class ProfileViewAssembler {
        assemble(subject, viewerId) ProfileDto
    }
    class AccountAccess {
        requireStudent()
        requireAdmin()
        lockStudents()
        lockLifecycle()
    }
    class DatabaseMutationLock {
        shared()
        exclusive()
    }
    class MatchRequestService
    class ConnectionService
    class ProfileViewService
    class StudyGroupService
    class GroupJoinRequestService
    class NotificationService
    class AdminUserService
    class StudentDeactivation
    class StudentDeletion
    class GroupClosure
    Student "0..1" --> "1" User : shared account primary key
    MatchRequest "*" --> "1" Student : sender
    MatchRequest "*" --> "1" Student : receiver
    Connection "*" --> "1" Student : studentA
    Connection "*" --> "1" Student : studentB
    StudyGroup "*" --> "1" Student : leader
    GroupJoinRequest "*" --> "1" Student : applicant
    GroupJoinRequest "*" --> "1" StudyGroup
    GroupMembership "*" --> "1" Student
    GroupMembership "*" --> "1" StudyGroup
    Notification "*" --> "1" Student : recipient
    ProfileDto <|.. PublicProfileDto
    ProfileDto <|.. ConnectedProfileDto
    ProfileViewService --> ProfileViewAssembler
    ProfileViewAssembler ..> PublicProfileDto : chooses
    ProfileViewAssembler ..> ConnectedProfileDto : chooses
    MatchRequestService --> AccountAccess
    ConnectionService --> AccountAccess
    StudyGroupService --> AccountAccess
    GroupJoinRequestService --> AccountAccess
    AdminUserService --> AccountAccess
    AccountAccess --> DatabaseMutationLock
    MatchRequestService --> NotificationService
    GroupJoinRequestService --> NotificationService
    AdminUserService --> StudentDeactivation
    AdminUserService --> StudentDeletion
    StudentDeactivation --> GroupClosure
    StudyGroupService --> GroupClosure
    GroupClosure --> NotificationService
```

Services coordinate transactions/authorization; entities own one-way state transitions.
Assemblers select response fields while entities remain internal. Validation and lifecycle
cleanup are separate collaborators, rather than logic in controllers. Value objects and
existing domain enums are reused. Collection fields/course/availability objects omitted
from this overview are shown in the ER diagram and actual source.

## Persisted relationships

```mermaid
erDiagram
    users ||--o| students : shared_id
    students ||--o{ student_courses : enrolled
    courses ||--o{ student_courses : course
    students ||--o{ student_study_goals : goals
    students ||--o{ availability_slots : weekly_slots
    courses o|--o{ students : optional_target
    students ||--o{ match_requests : sender
    students ||--o{ match_requests : receiver
    courses o|--o{ match_requests : optional_context
    students ||--o{ connections : student_a
    students ||--o{ connections : student_b
    courses ||--o{ study_groups : course
    students ||--o{ study_groups : leader
    study_groups ||--o{ study_group_goals : goals
    study_groups ||--o{ group_availability_slots : weekly_slots
    study_groups ||--o{ group_memberships : accepted_members
    students ||--o{ group_memberships : member
    study_groups ||--o{ group_join_requests : requests
    students ||--o{ group_join_requests : applicant
    students ||--o{ notifications : recipient
    study_groups ||--o| study_rooms : private_room
    study_rooms ||--o{ room_presences : tab_leases
    students ||--o{ room_presences : owner
    study_groups o|--o{ study_sessions : group_parent
    connections o|--o{ study_sessions : buddy_parent
    users {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar role
        boolean active
        bigint token_version
        timestamptz last_login_at
    }
    match_requests {
        bigint id PK
        bigint sender_id FK
        bigint receiver_id FK
        varchar status
        varchar origin
        bigint context_course_id FK
        varchar context_study_goal
    }
    connections {
        bigint id PK
        bigint student_a_id FK
        bigint student_b_id FK
        timestamptz ended_at
    }
    group_memberships {
        bigint id PK
        bigint study_group_id FK
        bigint student_id FK
        timestamptz joined_at
    }
    notifications {
        bigint id PK
        bigint recipient_id FK
        varchar type
        text message
        boolean read
        varchar resource_type
        bigint resource_id
        varchar event_key
    }
    study_rooms {
        bigint id PK
        bigint study_group_id FK
        bigint version
        bigint host_id FK
        bigint co_host_id FK
        varchar music_preset
        boolean music_playing
        varchar ambient_preset
        boolean ambient_playing
    }
    room_presences {
        bigint id PK
        bigint study_room_id FK
        bigint student_id FK
        uuid client_id
        timestamptz expires_at
    }
    study_sessions {
        bigint id PK
        bigint version
        uuid calendar_uid UK
        bigint study_group_id FK
        bigint connection_id FK
        timestamp start_local
        timestamp end_local
        varchar time_zone
        integer occurrence_count
        timestamptz start_at
        timestamptz end_at
        varchar status
    }
```

Natural collection keys are `(student_id,course_id)`, `(student_id,study_goal)` and
`(study_group_id,study_goal)`. Membership is unique per group/student. Partial unique
indexes protect unordered pending buddy pairs, unordered active connections and pending
group/applicant pairs while allowing terminal history. Non-null recipient/event keys are
unique. Notification resource IDs are safe navigation metadata, not cascading foreign keys;
permanent account cleanup explicitly removes links to deleted records. Matching config/
strategy tables exist in the baseline but are omitted from C's ER view.
E2 sessions require exactly one non-null group/connection parent. Room/student tab
leases are unique by `(study_room_id,student_id,client_id)`; distinct students,
not lease rows, determine current capacity.

## Request acceptance and contact disclosure

```mermaid
sequenceDiagram
    actor Receiver
    participant Controller as MatchRequestController
    participant Service as MatchRequestService
    participant Access as AccountAccess
    participant DB as PostgreSQL
    participant Events as NotificationService
    participant ProfileAPI as ProfileViewController
    participant Profiles as ProfileViewService
    participant Assembler as ProfileViewAssembler
    Receiver->>Controller: POST /match-requests/id/accept + JWT
    Controller->>Service: accept(id, principal.id)
    Service->>Access: shared lifecycle guard; receiver check; sorted account locks
    Access->>DB: lock participant users; recheck active account/version
    Service->>DB: lock request; read current status/active connection
    Service->>Service: MatchRequest.accept()
    Service->>DB: insert one symmetric Connection
    Service->>Events: notify sender, stable event key
    Events->>DB: insert recipient event
    Service->>DB: commit request + connection + notification
    Service-->>Controller: assembled MatchRequestDto
    Controller-->>Receiver: 200 ACCEPTED
    Note over Service,DB: Repeated/racing decisions conflict; any failure rolls back all writes
    Receiver->>ProfileAPI: GET student's profile + JWT
    ProfileAPI->>Profiles: view(subject, principal.id)
    Profiles->>DB: active viewer and subject
    Profiles->>Assembler: assemble(subject, viewer)
    Assembler->>DB: current relationship and public study data
    alt self or active buddy
        Assembler-->>Profiles: ConnectedProfileDto including contact
    else public / pending / ended / group-only
        Assembler-->>Profiles: PublicProfileDto with no contact field
    end
    Profiles-->>ProfileAPI: assembled ProfileDto
    ProfileAPI-->>Receiver: 200 + no-store
```

After disconnect, a later lookup selects the public shape for both sides. The client purges
its current private view and refetches; previously downloaded data cannot be erased remotely.

## Group approval and capacity

```mermaid
sequenceDiagram
    actor Leader
    participant API as GroupJoinRequestController
    participant Service as GroupJoinRequestService
    participant Access as AccountAccess
    participant DB as PostgreSQL
    participant Events as NotificationService
    Leader->>API: POST /groups/groupId/join-requests/id/accept
    API->>Service: accept(groupId, requestId, principal.id)
    Service->>Access: shared guard + sorted leader/applicant account locks
    Access->>DB: recheck account eligibility/version
    Service->>DB: lock group, then request
    Service->>Service: group leader/open; request belongs here and pending
    Service->>DB: recheck no membership and count accepted members
    alt accepted count below capacity
        Service->>Service: request.accept()
        Service->>DB: insert unique membership
        Service->>Events: applicant accepted event
        Events->>DB: insert event
        Service->>DB: commit all
        Service-->>Leader: accepted request DTO
    else full / closed / stale / unauthorized
        Service-->>Leader: safe 400/403/404/409; rollback
    end
    Note over Service,DB: Approval, capacity edits, removal and closure serialize on the same group row
```

Pending applicants do not consume seats. Creation inserts the leader membership. Closure
rejects pending applicants and emits one closure event per other member; it retains history.

## Account cleanup and transactional notifications

```mermaid
sequenceDiagram
    actor Admin
    participant API as AdminUserController
    participant Service as AdminUserService
    participant Access as AccountAccess
    participant Cleanup as Lifecycle collaborator
    participant Events as NotificationService
    participant DB as PostgreSQL
    Admin->>API: deactivate or DELETE account + JWT
    API->>Service: lifecycle(targetId, principal.id)
    Service->>Access: exclusive lifecycle guard
    Access->>DB: lock administrators and target; recheck actor/version
    Service->>Service: block removing own account
    alt deactivate
        Service->>Cleanup: StudentDeactivation.apply()
        Cleanup->>DB: end connections; cancel pending buddy requests
        Cleanup->>DB: close led groups; withdraw own pending applications; leave open groups
        Cleanup->>DB: revoke room leases and roles; cancel affected future sessions
        Service->>DB: inactive + increment token version
    else delete permanently
        Service->>Cleanup: StudentDeletion.apply()
        Cleanup->>DB: remove unsafe linked notifications and account dependencies
        Cleanup->>DB: remove led groups and their dependencies; remove profile
        Service->>DB: remove user
    end
    Cleanup->>Events: safe survivor events within same transaction
    Events->>DB: recipient-scoped events, stable unique keys
    Service->>DB: commit or roll back everything
    Service-->>Admin: updated DTO or 204
    Note over Access,DB: Shared writes cannot introduce a relationship during exclusive cleanup
    Note over Service,DB: Reactivation enables fresh login only; no relationship/token restoration
```

Read/read-all operations check the authenticated recipient, persist `read=true`, and are
idempotent. The badge counts all unread events independently of the currently selected
Requests/Groups filter. The backend-only RLS/grants migration closes alternate Supabase
browser data access; backend JDBC is the sole application access path.
