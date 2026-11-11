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

- **Checked:** every reply is validated before it is shown or saved, and rejected if it fails.
- **Labelled:** AI-written explanations and quiz questions are marked as possibly inaccurate.
- **Free tier:** Google may use submitted content, so only anonymous or student-provided
  material is sent.
- **Prompts:** versioned files in `backend/src/main/resources/prompts/`; a new explanation
  prompt version retires cached explanations.

## AI used to build the project

GPT models (through Codex) and Claude were used for planning, writing and reviewing code,
fixing bugs, tests, documentation and diagrams, and UI polish. The larger AI-assisted pieces
were the matching engine fixes, E1 match explanations, E2 rooms and sessions, the Study Room
tab, reactions and the focus forest, and the submission-readiness pass.

**Demo videos** (`docs/assets/video/`): Claude scripted, animated and edited both from screen
captures of the real app on fictional seeded data.

- **Narration** is synthetic: Chatterbox text-to-speech (Resemble AI) with a synthetic Kokoro
  reference voice. No real person's voice was used.
- **Music and sound effects** were generated in code. No third-party audio.
- **AI results on screen** (explanation, quiz, study plan, session roadmap, Pixel) are real
  Gemini replies produced while recording.
- Speech-to-text (faster-whisper) checked the narration was clear.

## Code review and responsibility

AI-written or not, the code was reviewed by the team that owns it: Team A reviewed the matching
engine and the Study Room tab, Team B the platform, profiles and AI study tools, and Team C
requests, connections, groups, admin, notifications, E1 match explanations and the E2 rooms and
sessions. Changes went in through pull requests. Every member reviewed the work they kept, can
explain the code in their area, and explained it to the team in physical meetings and demos.
