# AI usage

GPT models and Claude assisted with:

- **Planning:** comparing the project brief and issues, defining scope and evaluating design choices.
- **Implementation:** backend services, DTOs, validation and migrations; frontend screens and shared components, including matching integration.
- **Review and fixes:** checking privacy, permissions, state transitions, concurrency and form behaviour; reproducing and fixing defects.
- **Testing:** unit, HTTP/database and frontend regression tests, plus browser workflow verification.
- **Documentation:** README, API contracts, requirement coverage, design decisions and diagrams.
- **Interface and assets:** retro UI polish, accessibility checks and the original pixel-book favicon.
- **Matching engine review (`fix/team-a/matching-engine`):** Claude, at a Team C member's request, proposed and implemented the matching fixes, tests and decision report in [MATCHING_ENGINE_CHANGES.md](MATCHING_ENGINE_CHANGES.md), and a follow-up review's fixes. Team A owns the engine and must review and be able to explain it before merging.
- **E2 study experience:** private rooms, timers, presence, original layered audio, sessions/calendar, weather themes and related integration in PR #52; this is outside the compulsory baseline. Claude, at a Team C member's request, also reviewed PR #52 and made its room-polling, presence-locking, weather-cache, merge and README fixes.

Team members remain responsible for verifying retained work and explaining it in the viva.
Development assistance is separate from the application's E1 AI match-explanation feature.

## E1 AI match explanation (16 October 2026)

The application's E1 feature, separate from development assistance above.

- **Provider:** Google Gemini API, free tier, model `gemini-3.5-flash` (`app.ai.explanation.model`; `gemini-2.5-flash` until 18 October). Called only when a student clicks **Explain this match**, at most `daily-call-limit` times a day.
- **Sent:** only `ExplanationEvidence`: quality, total score, strategy, each criterion's rounded score, enabled flag and whether both students shared that preference (`known`), shared hours (nearest half hour), course code and name, shared goals, both study modes and both group-size preferences.
- **Never sent:** names, student or account IDs, emails, contact numbers, school, programme, year, messages, availability slots, connections or group memberships. `ExplanationEvidenceFactoryTest` and `ExplanationPromptBuilderTest` check this.
- **Request settings:** thinking is turned off (`thinkingBudget: 0`) so the short JSON reply is not cut off by the output-token limit.
- **Output checks:** JSON shape, length limit, and no probability, chance-of-success, guarantee or percentage claims. Anything else falls back to the matching engine's own explanation, labelled "Standard explanation".
- **Fallback observed (16 Oct):** in the development sandbox every request to Gemini failed (a network restriction of that environment, not a Gemini error), and the endpoint returned the standard explanation in under a second with no error shown to the student.
- **Real examples:** to be recorded by the owner from an unrestricted network: one STRONG, one GOOD and one FAIR match, each with its breakdown and an accuracy note.
- **Limitations:** free-tier prompts may be used by Google and read by reviewers; the daily limit is in memory and resets on restart; the prompt is English-only; wording can change when the model changes.
- **Development assistance:** Claude helped design, plan, implement and test E1 (backend `matchexplanation` package, Team A's `MatchService.evaluate`, the frontend panel). Charlize reviewed the design and plan; authors remain responsible for explaining every line.

Prompt template v2 (`backend/src/main/resources/prompts/match-explanation-v2.txt`). It replaced v1 on
16 October so that a preference one student left empty is described as not shared rather than as a
weak match; bumping the version also retires every explanation cached under v1.

```text
You explain study-buddy match results to a university student.

Below is anonymous evidence about one match between "you" (the student reading) and "this student".
Scores run from 0 to 1. A criterion with "enabled": false did not count towards the total.
A criterion with "known": false means at least one student did not share that preference,
so its score says nothing about how well you fit.
Quality is STRONG, GOOD or FAIR.

Write two or three short sentences that:
- start with the strongest enabled criteria, then mention the weakest enabled known one;
- describe a criterion with "known": false as not shared, never as a weak or poor match;
- mention the shared hours per week and the course when they are present;
- use only facts in the evidence, and never invent names, preferences or details;
- never describe the result as a probability, chance of success, guarantee or percentage.

Reply with JSON only, in exactly this shape: {"explanation": "<your sentences>"}

Evidence:
{{evidence}}
```
