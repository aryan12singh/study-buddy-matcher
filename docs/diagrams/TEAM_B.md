# Team B: platform and AI study tools

This companion covers Team B's two areas. The platform foundation — identity, authentication and
own-profile — already has its boundary and use cases in [PLATFORM.md](PLATFORM.md) and its
decisions in [DESIGN_DECISIONS.md](../DESIGN_DECISIONS.md#platform-and-own-profile-team-b), so it
is not repeated here. This document instead covers the AI study tools: Quiz Buddy, study plans and
the Pixel assistant. All three share one Gemini integration with match explanations (E1, Team C's
[E1.md](E1.md)), but each holds its own API key, model and daily allowance so one feature can never
spend another's. Presentation panel: [AI study tools](exports/ai-study-tools.svg); the
[API contract](../API_CONTRACT.md#quizzes-ai-study-assistant) defines the endpoints, and
[testing](../TESTING.md#e1-quizzes-study-plans-and-the-assistant) covers how they are tested.

## Use cases and permissions

```mermaid
flowchart LR
    Student([Active student]) --> Open[Open Quiz Buddy in the Study Room]
    Open --> Attach[Attach or paste notes, say a course/focus]
    Attach --> Write[Gemini writes ten questions]
    Write --> Take[Take the quiz, one attempt each]
    Take --> Review[Review answers once closed]
    Leader([Group leader, host or co-host]) --> StartGroup[Start a group quiz, optionally for a session]
    StartGroup --> Board[Members see a live leaderboard]
    Student --> Plan[Ask for a personal study plan]
    Plan --> Roadmap[Gemini writes a weekly roadmap from the material]
    Student --> Chat[Message Pixel]
    Chat --> Answer[Rules answer common questions; AI reads only what rules cannot]
    Take --> Privacy[Material never stored; only the questions are kept]
```

An invisible quiz is 404, so its ID reveals nothing; a group's quiz list is 403, like its room
and sessions. Answers and the correct option stay server-side until a quiz closes. A student's
study plan and a session's plan are visible only to their owner or current participants.

## OO collaborators

`QuizWriter` depends on `AiClient`, not `GeminiClient` directly, so tests supply a fake that
returns fixed replies or fails on demand. `AiAssistantConfiguration` builds the one `GeminiClient`
instance that quizzes and study plans share, each client holding its own `GeminiSettings` and
`AiQuota` from `app.ai.assistant.*`. The chat assistant and E1 build their own instances of the
same `GeminiClient` class from their own settings, so a quiz never spends the chat's or E1's
allowance, and vice versa.

```mermaid
classDiagram
    direction TB
    class QuizController {
        +startPersonal(actor, input, files) QuizDto
        +start(groupId, actor, input, files) QuizDto
    }
    class QuizCreationService {
        +startPersonal(actorId, input, files) QuizDto
        +startForGroup(groupId, actorId, input, files) QuizDto
    }
    class QuizService {
        +preparePersonalStart(actorId, input) QuizStartRequest
        +savePersonalStarted(actorId, request, title, draft) QuizDto
    }
    class QuizAccess {
        +requireMember(groupId, actorId) StudyGroup
        +requireHost(groupId, actorId) StudyGroup
        +requireReadable(quiz, actorId) void
    }
    class StudyMaterialReader {
        +read(files, notesText) StudyMaterial
    }
    class QuizWriter {
        +write(studentId, course, focus, material) QuizDraft
    }
    class QuizPromptBuilder {
        +PROMPT_VERSION String
        +build(course, focus, material) String
    }
    class AiClient {
        <<interface>>
        +generateJson(studentId, prompt, attachments) String
    }
    class GeminiClient {
        +generateJson(studentId, prompt, attachments) String
    }
    class AiQuota {
        +tryAcquire(studentId) boolean
        +pauseAfterRateLimit() void
    }
    class QuizOutputValidator {
        +validate(raw) Optional~QuizDraft~
    }
    QuizController --> QuizCreationService
    QuizCreationService --> QuizService : short checking read, then short write
    QuizCreationService --> QuizAccess : group permission checks
    QuizCreationService --> StudyMaterialReader
    QuizCreationService --> QuizWriter
    QuizWriter --> QuizPromptBuilder
    QuizWriter --> AiClient
    QuizWriter --> QuizOutputValidator
    GeminiClient ..|> AiClient
    GeminiClient --> AiQuota : spends allowance, pauses after HTTP 429
```

## Persisted domain

A quiz owns its questions outright (cascade-deleted with it); an attempt references a quiz and a
student but is owned by neither. The database allows only one attempt per student per quiz, so
starting twice returns the existing attempt rather than creating a new one.

```mermaid
classDiagram
    direction LR
    class Quiz {
        +id Long
        +scope QuizScope
        +status QuizStatus
        +title String
        +closesAt Instant
        +closedAt Instant
        +statusAt(now) QuizStatus
        +close(now) void
    }
    class QuizQuestion {
        +id Long
        +position int
        +prompt String
        +options List~String~
        +correctOption int
        +explanation String
    }
    class QuizAttempt {
        +id Long
        +startedAt Instant
        +submittedAt Instant
        +score Integer
        +total Integer
        +answers Map~Long, Integer~
        +submit(chosenOptions, score, total, now) void
        +isSubmitted() boolean
    }
    class StudyGroup
    class Student
    class Course
    Quiz "0..1" --> "0..1" StudyGroup : group quiz
    Quiz "0..1" --> "0..1" Student : personal quiz owner
    Quiz "1" --> "1" Course
    Quiz "1" *-- "10" QuizQuestion : ordered, cascade-deleted
    QuizAttempt "many" --> "1" Quiz
    QuizAttempt "many" --> "1" Student
```

Study plans follow the same owned-children shape: `StudyPlan` *(scope, course, block calendar,
source filenames/topics)* cascade-owns its `StudyPlanEntry` rows *(sequence, scheduled date,
title, planned minutes, completed, tasks)*, and belongs to either a `Student` (personal) or a
`StudySession` (session), never both.

## Lifecycles

`Quiz` exposes `close(now)` and no status setter, so an illegal transition cannot be written from
outside the class. A quiz's own status and a member's attempt state are independent: the quiz can
stay `OPEN` while one member has submitted and another has not started.

```mermaid
stateDiagram-v2
    direction LR
    [*] --> OPEN : Quiz written and saved
    OPEN --> CLOSED : closing time passes, or<br/>a host/owner closes it early
    CLOSED --> [*]

    state OPEN {
        direction LR
        [*] --> NotStarted : member has not opened it
        NotStarted --> Started : QuizAttempt.start()<br/>(timer begins)
        Started --> Submitted : QuizAttempt.submit()<br/>(answers, score recorded)
        Submitted --> [*]
    }
```

`close(now)` is idempotent and records `closedAt` only when closed early, so takers who already
finished keep their results; a group quiz past its `closesAt` reads as closed without a
scheduler. `QuizAttempt.submit()` may run exactly once — a second call throws, reported as 409.

## Quiz request sequence and early exits

Deliberately not transactional: creation runs as a short checking read, the AI call, then a short
write, so no database connection or account lock is held while Gemini answers (which can take
many seconds for a PDF). The `ready()` check on `AiAssistantProperties` — on by default only when
`application-local.yml` sets `enabled: true` and `api-key` — runs before the quota, so a
switched-off feature never spends a student's daily allowance.

```mermaid
sequenceDiagram
    actor Student
    participant UI as QuizBotChat
    participant API as QuizController
    participant Creation as QuizCreationService
    participant Quiz as QuizService
    participant Material as StudyMaterialReader
    participant Writer as QuizWriter
    participant AI as AiClient / GeminiClient
    participant Quota as AiQuota
    participant Gemini as Google Gemini
    participant Validator as QuizOutputValidator
    Student->>UI: Attach notes/PDF, say focus + course
    UI->>API: POST /api/quizzes (multipart: files, notesText, focus, courseId)
    API->>Creation: startPersonal(actorId, input, files)
    Creation->>Quiz: preparePersonalStart(actorId, input)
    Quiz-->>Creation: QuizStartRequest (course and student checked)
    Creation->>Material: read(files, notesText)
    Material-->>Creation: StudyMaterial
    Creation->>Writer: write(studentId, course, focus, material)
    Writer->>Writer: QuizPromptBuilder.build(course, focus, material)
    Writer->>AI: generateJson(studentId, prompt, attachments)
    break Switched off, or no api-key configured
        AI-->>Writer: throw AiUnavailableException
        Writer-->>Creation: propagate
        Creation-->>API: propagate
        API-->>UI: 503 AI_UNAVAILABLE
        UI-->>Student: "try again" message
    end
    AI->>Quota: tryAcquire(studentId)
    break Over the daily or per-student limit, or pausing after a rate limit
        Quota-->>AI: false
        AI-->>Writer: throw AiUnavailableException
        Writer-->>Creation: propagate
        Creation-->>API: propagate
        API-->>UI: 503 AI_UNAVAILABLE
        UI-->>Student: "try again" message
    end
    AI->>Gemini: POST /models/{model}:generateContent (x-goog-api-key header)
    Gemini-->>AI: JSON reply, or an HTTP error / rate limit
    AI-->>Writer: reply text
    Writer->>Validator: validate(reply)
    break Reply fails the quiz's shape checks
        Validator-->>Writer: empty
        Writer-->>Creation: propagate
        Creation-->>API: propagate
        API-->>UI: 503 AI_UNAVAILABLE
        UI-->>Student: "try again" message
    end
    Validator-->>Writer: QuizDraft
    Writer-->>Creation: QuizDraft
    Creation->>Quiz: savePersonalStarted(actorId, request, title, draft)
    Quiz-->>Creation: QuizDto (quiz and questions saved)
    Creation-->>API: QuizDto
    API-->>UI: 201 Created
    UI-->>Student: "Your quiz is ready!"
```

## Study plans and the Pixel assistant

Both reuse the collaborators above with their own prompt builder and output validator; neither
has its own sequence diagram yet. Full decisions are in
[DESIGN_DECISIONS.md](../DESIGN_DECISIONS.md#study-plans) and
[DESIGN_DECISIONS.md](../DESIGN_DECISIONS.md#pixel-study-assistant).

| Feature | What differs from the quiz flow |
| --- | --- |
| Study plan | `StudyPlanCalendar` frames the weeks/meetings server-side before Gemini plans only those blocks; `StudyPlanOutputValidator` checks every block is covered and fits its limits |
| Pixel assistant | `QuickIntentReader` answers common questions with rules and no AI call at all; only a message needing a time, date or other free-text detail reaches `IntentParser`. The AI classifies intent and extracts details only — every fact in the reply still comes from existing services, so the model cannot invent a buddy, time or task |

## Privacy, limits and fallback constraints

- Uploaded or pasted material is sent to Gemini for that one request and never stored. A quiz
  keeps only its questions and a title; a study plan keeps only the extracted topic titles and
  file names; Pixel keeps no history at all.
- Prompt injection: student text and material are marked as data, never instructions.
  `PromptTemplate` fills `{{slot}}` markers in one pass over the template only.
- Any failure — switched off, over quota, rate limited, a timeout or a reply that fails its
  output checks — is `AiUnavailableException`, surfaced as `503 AI_UNAVAILABLE`. Logs never
  contain the prompt, file, reply or key.
- `app.ai.assistant.*` is off unless `application-local.yml` sets `enabled: true` and `api-key`;
  the chat assistant shares that key and switch but keeps its own lighter model, quota and
  rate-limit pause under `app.ai.assistant.chat.*`.
