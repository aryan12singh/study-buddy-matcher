# Team C implementation diagrams

These diagrams describe the implemented composition/DTO/transaction model. The earlier
discussion's Student Leader inheritance sketch is not the implementation: `Student`
composes `User` with `@MapsId`, and `StudyGroup.leader` refers to an ordinary student.
`Role` contains only STUDENT and ADMIN. Matching strategies/scorers remain Team A scope.
The [API contract](../API_CONTRACT.md) contains full DTO fields/endpoints and the
[decisions](../DESIGN_DECISIONS.md) explain privacy, lifecycle and locking choices.

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
    Lifecycle --> Guard[Protect self and last active administrator]
```

An incoming request can be answered only by its receiver; a connection can be ended only
by a participant. Leader authority is checked against the URL's particular group. Admin
authority does not bypass profile contact privacy. Server checks remain necessary when
the UI hides or disables unavailable actions.

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
```

Natural collection keys are `(student_id,course_id)`, `(student_id,study_goal)` and
`(study_group_id,study_goal)`. Membership is unique per group/student. Partial unique
indexes protect unordered pending buddy pairs, unordered active connections and pending
group/applicant pairs while allowing terminal history. Non-null recipient/event keys are
unique. Notification resource IDs are safe navigation metadata, not cascading foreign keys;
permanent account cleanup explicitly removes links to deleted records. Matching config/
strategy tables exist in the baseline but are omitted from C's ER view.

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
        Cleanup->>DB: close led groups; reject pending applications; leave open groups
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
