# AI usage

## AI inside the app (E1)

The backend calls Google Gemini (free tier) from two Google projects, so quotas stay separate.
With AI off, over its daily allowance or failing, each screen falls back to a standard answer
or the manual path.

| Feature (model key) | Sent to Gemini | Never sent |
| --- | --- | --- |
| Match explanations (`app.ai.explanation.model`) | Anonymous `ExplanationEvidence`: quality, rounded scores and weights, shared hours, shared courses and goals, study modes, group-size preferences | Names, IDs, emails, contact numbers, school, programme, year, messages, timetables, connections, memberships |
| Quiz Buddy and study plans (`app.ai.assistant.model`) | The student's uploaded files and file names or pasted notes, course code and name, quiz focus or plan settings | Contact numbers, emails, other students' data |
| Pixel (`app.ai.assistant.chat.model`) | The student's own message, to classify it | Everything else; answers come from the app's services and templates |

- **Checked before shown:** shape, length and banned wording (explanations); question count,
  plan weeks and minutes (quizzes, plans). A reply that fails is rejected.
- **Labelled:** the checks bound shape, not truth, so explanations and quiz questions are
  marked as possibly inaccurate.
- **Free-tier terms:** Google may use submitted content to improve its products, human
  reviewers may read it, and users must be 18 or over.
- **Prompts:** versioned files in `backend/src/main/resources/prompts/`; a new explanation
  prompt version retires cached explanations.

## AI used to build the project

Tools: GPT models (through Codex) and Claude.

| Used for | Detail |
| --- | --- |
| Planning | Comparing the brief and issues, scope, design choices |
| Code | Backend services, DTOs, validation, migrations; frontend screens and shared components |
| Review and fixes | Privacy, permissions, state transitions, concurrency, forms; reproducing and fixing defects |
| Tests | Unit, HTTP/database and frontend regression tests; browser workflow checks |
| Documentation | README, API contract, requirement coverage, design decisions, diagrams, `AGENTS.md` |
| Interface | Retro UI polish, accessibility checks, the original pixel-book favicon |
| Matching engine fixes | Claude proposed and built fixes and tests at a Team C member's request; Team A reviewed them ([decisions](DESIGN_DECISIONS.md#matching-decisions-team-a)) |
| E1 match explanations | Claude helped design, plan, build, review and test `matchexplanation`, `MatchService.evaluate` and the panel; Charlize reviewed the design, plan and findings |
| E2 rooms and sessions | Help with rooms, timers, presence, sessions, calendar and weather themes; a review fixed room polling, presence locking and the weather cache |
| Study Room tab | Codex: personal Pomodoro, groups sidebar, original CSS scenery, reduced-motion-aware animation, reusing the group-room APIs |
| Reactions and focus forest | Claude, at Charlize's request: feasibility check, plan, endpoint and tray, focus crediting, tree tiers, forest page, original pixel sprites, demo data, tests |
| Submission readiness | Fixes, refactors, documentation and screenshots with Claude; reviewed by the team |

**Demo videos** (`docs/assets/video/`): Claude scripted, animated and edited both from screen
captures of the real app on fictional seeded data.

- **Narration** is synthetic: Chatterbox text-to-speech (Resemble AI) with a synthetic Kokoro
  reference voice. No real person's voice was used.
- **Music and sound effects** were generated in code. No third-party audio.
- **AI results on screen** (explanation, quiz, study plan, session roadmap, Pixel) are real
  Gemini replies produced while recording.
- Speech-to-text (faster-whisper) checked the narration was clear.

## Responsibility

Every member reviewed the work they kept and explained it to the team in physical meetings and
demos.
