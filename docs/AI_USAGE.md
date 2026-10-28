# AI usage

This page covers two separate things: AI assistance used while developing the project, and the
AI features built into the application.

## Development assistance

GPT models (through Codex) and Claude assisted the team with:

| Area | What the assistance covered |
| --- | --- |
| Planning | Comparing the brief and issues, defining scope and evaluating design choices |
| Implementation | Backend services, DTOs, validation and migrations; frontend screens and shared components |
| Review and fixes | Privacy, permissions, state transitions, concurrency and form behaviour; reproducing and fixing defects |
| Testing | Unit, HTTP/database and frontend regression tests, and browser workflow checks |
| Documentation | README, API contract, requirement coverage, design decisions and diagrams |
| Interface and assets | Retro UI polish, accessibility checks and the original pixel-book favicon |

Specific pieces of work:

- **Matching engine fixes (Team A's engine).** Claude proposed and implemented fixes and tests
  at a Team C member's request; the decisions are recorded in
  [design decisions](DESIGN_DECISIONS.md#matching-decisions-team-a). Team A reviewed them.
- **E1 AI match explanations.** Claude helped design, plan, implement, review and test the
  backend `matchexplanation` package, `MatchService.evaluate` and the frontend panel. Charlize
  reviewed the design, plan and review findings.
- **E2 collaborative study.** Assistance with rooms, timers, presence, sessions and calendar,
  weather themes and their integration, and a review that fixed room polling, presence locking
  and the weather cache.
- **Study Room tab.** Codex added the separate tab with a personal Pomodoro, groups sidebar,
  original CSS scenery and a reduced-motion-aware progress animation, reusing the existing
  group-room APIs and components.

Every member reviewed the work they retained and explained it to the team in physical meetings
and demos.

## AI in the application

The application calls Google Gemini (free tier) in two places, on separate Google projects so
they do not share a quota. Both work without AI: with it off, over the daily allowance or
failing, the screen falls back to a standard answer or the manual path.

| Feature | Model key | What is sent | Never sent |
| --- | --- | --- | --- |
| E1 match explanation | `app.ai.explanation.model` | Anonymous `ExplanationEvidence` only: quality, rounded criterion scores and weights, shared hours, shared courses and goals, study modes and group-size preferences | Names, IDs, emails, contact numbers, school, programme, year, messages, timetables, connections, memberships |
| Quizzes and study plans | `app.ai.assistant.model` | The student's uploaded files (and their names) or pasted notes, the course code and name, and the quiz focus or plan settings | Contact numbers, emails, other students' data |
| Pixel assistant | `app.ai.assistant.chat.model` | The student's own message only, to classify it | Everything else: answers come from the app's services and templates |

All Gemini output is checked before it is shown (shape, length and banned wording for E1;
question count, plan weeks and minutes for quizzes and plans) and rejected otherwise. On the
free tier Google may use submitted content to improve its products and human reviewers may read
it, and users must be 18 or over. Checks bound the output's shape; they cannot prove every
sentence is accurate, so explanations and quiz questions are labelled as possibly inaccurate. Prompts live in
`backend/src/main/resources/prompts/`; changing E1's wording bumps its prompt version, which
retires cached explanations.
