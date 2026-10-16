# Platform and whole-system diagrams

These diagrams connect the compulsory Team B authentication/profile/data foundation with
Team A matching and Team C requests, privacy, groups, notifications and administration.
They describe the compulsory implementation and this branch's E2 integration.
E1 AI explanations remain a separate extension. Source diagrams do not establish
that E2 has been deployed or accepted by the team.

Detailed OO and transaction diagrams remain in [TEAM_A.md](TEAM_A.md) and [TEAM_C.md](TEAM_C.md).
The [API contract](../API_CONTRACT.md), [decisions](../DESIGN_DECISIONS.md),
[coverage map](../REQUIREMENT_COVERAGE.md) and [baseline test record](../BASELINE_VERIFICATION.md) explain fields,
validation choices and verification limits. Export these diagrams legibly and review them
with the team before using them in the presentation.

## Compulsory use cases and system boundary

```mermaid
flowchart LR
    Visitor([Visitor])
    Student([Active student])
    Leader([Student leading this group])
    Admin([Active administrator])
    subgraph System[Study Buddy Matcher compulsory application]
        Register[Register student account]
        Login[Sign in]
        OwnProfile[Read and edit own profile and preferences]
        Availability[Replace or clear own weekly availability]
        Home[Read Home activity and matching preview]
        Search[Choose course or goal and find ranked buddies]
        Profile[Read student profile with contact privacy]
        Buddy[Send and answer buddy requests]
        Connection[View and end own connections]
        Groups[Browse and create course groups]
        Apply[Apply to a group and read application state]
        Manage[Edit group and decide membership requests]
        Close[Remove members or close led group]
        Events[Read and mark own notifications]
        Accounts[Create edit and manage accounts]
        Lifecycle[Deactivate reactivate or permanently delete]
        Config[Configure matching strategy criteria and weights]
    end
    Visitor --> Register
    Visitor --> Login
    Student --> OwnProfile
    Student --> Availability
    Student --> Home
    Student --> Search
    Student --> Profile
    Student --> Buddy
    Student --> Connection
    Student --> Groups
    Student --> Apply
    Student --> Events
    Leader --> Manage
    Leader --> Close
    Admin --> Accounts
    Admin --> Lifecycle
    Admin --> Config
```

Leadership is a per-group relationship held by a student, not a third JWT role. The only
account roles are STUDENT and ADMIN. Account/receiver/participant/leader checks run on the
server; hidden or disabled controls do not confer authority. Administrator access and group
membership do not grant private contact reads.

## Runtime and layer boundaries

```mermaid
flowchart LR
    Browser[React browser app]
    Client[Shared authenticated API client]
    Security[Spring Security JWT and role filter]
    Controllers[Feature controllers]
    Services[Feature application services]
    Assemblers[DTO assemblers and privacy policy]
    Domain[Domain objects and matching value objects]
    Repositories[JPA repositories]
    Database[(PostgreSQL / Supabase)]
    Browser --> Client
    Client -->|JSON over HTTP| Security
    Security --> Controllers
    Controllers -->|validated request and actor ID| Services
    Services --> Domain
    Services --> Repositories
    Services --> Assemblers
    Repositories --> Database
    Assemblers -->|response DTO| Services
    Services -->|DTO| Controllers
    Controllers -->|JSON DTO| Client
```

The browser uses the Spring API. Application tables/sequences deny browser roles, with RLS
enabled and no browser policies. Services may use repositories and domain entities internally;
controllers do not inject repositories and services do not return entities to the API. The
ordered SQL migrations define project-specific integrity/adoption/access rules beyond the
mapped DDL that Hibernate may update.

## E2 experience and external boundaries

```mermaid
flowchart LR
    Member[Accepted active group member]
    Buddy[Participant in active buddy connection]
    Leader[Group leader]
    Host[Present assigned host or co-host]
    subgraph E2[Authenticated E2 application]
        Room[Private group room and live presence]
        Timer[Authoritative shared Pomodoro]
        Audio[Shared music and ambient selection]
        LocalAudio[Local consent volume and mute]
        Sessions[Dated group and buddy sessions]
        Suggestions[Mutual weekly availability]
        Home[Actual Home and agenda data]
        Events[Recipient session notifications]
        Calendar[Private UTC calendar snapshot]
        Weather[Cached campus-weather DTO]
        Theme[Original local pixel atmosphere]
    end
    Member --> Room
    Member --> Sessions
    Buddy --> Sessions
    Leader -->|assign roles| Host
    Leader -->|manage group series| Sessions
    Host --> Timer
    Host --> Audio
    Room --> Timer
    Audio --> LocalAudio
    Sessions --> Suggestions
    Sessions --> Home
    Sessions --> Events
    Sessions --> Calendar
    Calendar --> Offline[Independent calendar import]
    OpenMeteo[Open-Meteo service] --> Weather
    Weather --> Theme
    Theme --> Room
```

Room leases, durable timer/audio state and dated sessions have distinct lifecycles.
The browser reconciles authenticated HTTP snapshots; original native audio runs
locally after consent. Session availability reuses Team A's interval maths and
does not modify matching rankings. Calendar downloads contain no attendee/contact/
token payload and provide no OAuth/live client sync. The optional weather request
sends only fixed configured campus coordinates/options from the backend, with
bounded timeout/cache and local selectable fallback; no student's device location
or profile is sent to the provider.

Membership removal, closed groups, ended connections and inactive accounts are
server authorization boundaries on every room/session request. Lifecycle changes
also revoke leases/roles and cancel affected future schedules; prior offline
calendar copies cannot be revoked. E2 test/deployment/import acceptance remains
separate in [E2_TESTING.md](../E2_TESTING.md).

## Identity, profile and cross-team domain relationships

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
    }
    class Student {
        Long id
        User user
        String name
        String school
        String programme
        Integer yearOfStudy
        String contactNumber
        StudyMode preferredStudyMode
        Integer preferredGroupSizeMin
        Integer preferredGroupSizeMax
        Set studyGoals
    }
    class Course {
        Long id
        String code
        String name
    }
    class AvailabilitySlot {
        DayOfWeek dayOfWeek
        LocalTime startTime
        LocalTime endTime
    }
    class MatchRequest
    class Connection
    class StudyGroup
    class GroupMembership
    class GroupJoinRequest
    class Notification
    class MatchingStrategySetting {
        MatchingStrategyType activeStrategy
        Set enabledCriteria
        double minScore
        long configVersion
        boolean hasOverride
    }
    class MatchingConfig
    class MatchingConfigService
    class StudyRoom
    class RoomPresence
    class StudySession
    Student "0..1" --> "1" User : shared MapsId identity
    Student "*" --> "*" Course : courses taken
    Student "*" --> "0..1" Course : target course
    AvailabilitySlot "*" --> "1" Student : own weekly blocks
    MatchRequest "*" --> "1" Student : sender
    MatchRequest "*" --> "1" Student : receiver
    Connection "*" --> "2" Student : participants
    StudyGroup "*" --> "1" Student : leader
    StudyGroup "*" --> "1" Course : course
    GroupMembership "*" --> "1" StudyGroup
    GroupMembership "*" --> "1" Student
    GroupJoinRequest "*" --> "1" StudyGroup
    GroupJoinRequest "*" --> "1" Student : applicant
    Notification "*" --> "1" Student : recipient
    StudyGroup "1" --> "0..1" StudyRoom : private room
    StudyRoom "1" --> "0..*" RoomPresence : tab leases
    RoomPresence "*" --> "1" Student : authenticated owner
    StudySession "*" --> "0..1" StudyGroup : group parent
    StudySession "*" --> "0..1" Connection : buddy parent
    MatchingConfigService ..> MatchingStrategySetting : singleton revision and enabled set
    MatchingConfigService ..> MatchingConfig : weights per strategy and criterion
```

`Student` composes an identity rather than inheriting `User`; administrators have no mandatory
student fields. Group membership and connection are separate consent relationships. Matching
reads active profiles and canonical weekly intervals but owns neither account lifecycle nor
contact visibility. Matching settings above are persistent admin configuration, not a student
profile's preferences. Team A's diagram defines the scorer/strategy interfaces and immutable
`TimeSlot`/`MatchScore` objects; Team C's diagram defines transition and lifecycle behavior.

## Authentication and own-profile service boundary

```mermaid
classDiagram
    class AuthController
    class AuthService
    class AccountCreation
    class AccountAssembler
    class PasswordEncoder {
        <<interface>>
    }
    class JwtTokenService
    class JwtAuthenticationFilter
    class AccountAccess
    class ProfileController
    class ProfileService
    class MyProfileAssembler
    class AvailabilityRules
    class ProfileProperties
    class UserRepository
    class StudentRepository
    class CourseRepository
    class AvailabilitySlotRepository
    class CurrentAccountDto
    class AuthResultDto {
        String token
        Instant expiresAt
        CurrentAccountDto account
    }
    class MyProfileDto
    class AvailabilitySlotDto
    AuthController --> AuthService
    AuthService --> AccountCreation
    AuthService --> AccountAssembler
    AuthService --> PasswordEncoder
    AuthService --> JwtTokenService
    AuthService --> AccountAccess
    AuthService --> UserRepository
    AuthService --> StudentRepository
    AuthService --> AuthResultDto
    AccountCreation --> UserRepository
    AccountCreation --> StudentRepository
    AccountCreation --> PasswordEncoder
    AccountAssembler --> CurrentAccountDto
    JwtAuthenticationFilter --> JwtTokenService
    JwtAuthenticationFilter --> UserRepository
    ProfileController --> ProfileService
    ProfileService --> AccountAccess : locked current student on writes
    ProfileService --> CourseRepository
    ProfileService --> AvailabilitySlotRepository
    ProfileService --> AvailabilityRules
    ProfileService --> ProfileProperties
    ProfileService --> MyProfileAssembler
    MyProfileAssembler --> MyProfileDto
    MyProfileAssembler --> AvailabilitySlotDto
```

Registration atomically creates `User` and `Student` and permits only the STUDENT role.
Login validates the current identity/password/account state before issuing a token and
recording successful sign-in. JWT account ID, role, expiry and version are checked against
the active account on protected requests. Deactivation invalidates existing tokens;
reactivation requires a fresh sign-in and does not restore ended relationships.

## Own-week replacement and alternatives

```mermaid
sequenceDiagram
    actor Student
    participant UI as Profile editor
    participant Filter as JWT and role filter
    participant API as ProfileController
    participant Service as ProfileService
    participant Access as AccountAccess
    participant Rules as AvailabilityRules
    participant Repo as AvailabilitySlotRepository
    participant DB as PostgreSQL
    Student->>UI: edit weekly time text
    alt raw draft is invalid or incomplete
        UI-->>Student: field error and save blocked
        Note over Student,UI: Dirty raw text also participates in exit warning
    else complete draft
        UI->>Filter: PUT /api/profile/me/availability with JWT and slots
        alt invalid revoked or missing token
            Filter-->>UI: 401 problem
        else wrong role
            Filter-->>UI: 403 problem
        else active student
            Filter->>API: verified actor and request
            API->>Service: replaceAvailability(actorId, request)
            Service->>Service: acquire shared mutation guard
            Service->>Access: lock current student and recheck actor
            Access->>DB: lock identity before collection replacement
            Service->>Rules: validate whole-minute same-day non-overlapping slots
            alt invalid saved-slot payload
                Service-->>API: 400 INVALID_INPUT with field errors
                API-->>UI: 400 problem
            else valid week including empty
                Service->>Repo: delete old slots then insert replacement
                Repo->>DB: replace inside transaction
                Service->>Service: assemble ordered slot DTOs
                Service-->>API: List of AvailabilitySlotDto
                API-->>UI: 200 complete saved week
                UI-->>Student: saved state and updated preview
            end
        end
    end
```

Concurrent replacements serialize on the same student's write boundary. Each completed
response represents one valid whole week; it must not contain a mixture of concurrent inputs.
Own profile course/goal collection replacements use that boundary too. Group schedules reuse
the complete time-slot shape and draft protection but allow same-day overlap.

## Matching boundary alternatives

This supplements Team A's detailed successful scoring/ranking sequence with the entry/error/
empty alternatives required for an integrated demo.

```mermaid
sequenceDiagram
    actor Student
    participant UI as Find buddies or Home
    participant Filter as JWT and role filter
    participant API as MatchController
    participant Service as MatchService
    participant DB as PostgreSQL
    Student->>UI: choose course or goal and search
    UI->>Filter: GET /api/matches with JWT and criteria
    alt missing invalid expired or revoked token
        Filter-->>UI: 401 problem
        UI-->>Student: sign-in or session-expired state
    else non-student role
        Filter-->>UI: 403 problem
        UI-->>Student: permission error
    else active student
        Filter->>API: verified actor and typed criteria
        API->>Service: search(criteria, actorId)
        alt missing course and goal or invalid criteria
            Service-->>API: 400 INVALID_INPUT
            API-->>UI: 400 problem with safe details
            UI-->>Student: validation/error state
        else valid criteria
            Service->>DB: one settings and candidate snapshot
            Service->>Service: eligibility filters, scoring, strategy order and limit
            alt no eligible result
                Service-->>API: versioned result with empty matches
                API-->>UI: 200 result DTO
                UI-->>Student: empty state and filter guidance
            else eligible results
                Service->>Service: assemble public score and request-state DTOs
                Service-->>API: versioned bounded result
                API-->>UI: 200 result DTO
                UI-->>Student: ranked cards and shared-time breakdown
            end
        end
    end
```

Malformed enum/numeric parameters may fail during controller binding before the service call;
the shared problem boundary still returns 400. Discovery DTOs never contain contact, email,
credentials or entities. An unexpected failure shows retryable error state, not fabricated
matches. `MatchingHttpTest` and matching rendered tests cover these branches.

## Other compulsory alternatives

| Flow | Empty / invalid / unauthorized alternative | Evidence |
| --- | --- | --- |
| Registration/login | Creation missing fields or short/oversize password → 400; duplicate normalized email → 409; incorrect or otherwise-valid over-byte login password → 401; correct password for inactive account → 403 | `AccountCreationTest`, `AccountHttpTest`, auth rendered tests |
| Own profile | Unknown/excess courses or missing preferences → 400; other role → 403; raw time draft remains local until complete | Profile/availability/editor tests and compulsory record |
| Buddy requests | No requests → empty list; self/duplicate/connected pair or repeat decision → conflict; wrong receiver → 403 | `BuddyHttpTest`, request/connection tests |
| Privacy | Stranger/pending/group-only viewer → public DTO; inactive or missing subject → 404; after disconnect → contact withheld | Privacy assembler and HTTP tests |
| Groups | No browse result → empty state; full/closed/repeated request → conflict; invalid capacity/time → 400; non-leader decision → 403 | `GroupHttpTest`, group and rendered tests |
| Notifications | Empty list → empty state; another recipient's mark-read → 403; failed event write rolls back triggering transition | Notification and HTTP rollback/direction tests |
| Admin accounts | Student/unauthenticated caller rejected; self-removal → conflict; removal never leaves an unusable last admin; deleted dependencies are removed safely | `AccountHttpTest`, admin/JWT tests |

The [coverage map](../REQUIREMENT_COVERAGE.md#compulsory-integrated-acceptance-map) links the
cross-feature checklist. Human cross-team review, author explanations, hosted rollout, Figma/projector acceptance
and submission remain separately recorded gates. The contact/learning-style baseline contract
is recorded in the decisions; no professor confirmation is claimed.
