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
- **Study Room entry:** Codex added the separate room tab and, following the user's visual reference, a personal Pomodoro with a groups sidebar and original CSS scenery, plus a gentle running-state progress-icon animation that respects reduced motion. It reused the existing group-room APIs, timer calculations and UI components, with navigation, loading/error and personal-timer regression checks. Human review remains pending.

Team members remain responsible for verifying retained work and explaining it in the viva.
Development assistance is separate from the application's E1 AI match-explanation feature.

## E1 AI match explanation (updated 18 October 2026)

The application's E1 feature, separate from development assistance above.

- **Provider:** Google Gemini API, free tier, model `gemini-3.5-flash` (`app.ai.explanation.model`). `gemini-2.5-flash` was used until 18 October; Google now returns 404 for it on new projects. Called only when a student clicks **Explain this match** and the answer is not already cached.
- **Free-tier limits (observed 18 October):** 20 generate requests per day per project per model (`GenerateRequestsPerDayPerProjectPerModel-FreeTier`), plus a per-minute limit. The app stops at `daily-call-limit` (18) and pauses for `rate-limit-cooldown` (60s) after any 429. Usage: https://ai.dev/rate-limit.
- **Sent:** only `ExplanationEvidence`, written in plain words by `ExplanationVocabulary`: quality, total score, each criterion's rounded score, weight, enabled flag and whether both students shared that preference (`known`), shared hours (nearest half hour), scoring and shared course codes and names, the searched goal, shared goals, both study modes and both group-size preferences. The admin's strategy name is not sent.
- **Never sent:** names, student or account IDs, emails, contact numbers, school, programme, year, messages, availability slots, connections, group memberships or the card's `evaluationId`. `ExplanationEvidenceFactoryTest`, `ExplanationPromptBuilderTest` and `ExplanationVocabularyTest` check this.
- **Request settings:** JSON reply, `maxOutputTokens: 200`, thinking turned off (`thinkingBudget: 0`). Without it a reply stopped after 189 thinking tokens. Timeout 5s for connect and for read.
- **Output checks:** JSON shape and length; no success, likelihood, chance, probability, guarantee or percentage wording; no leftover code names (`ONE_TO_ONE`, `STRONG`); course codes only from the evidence (hyphenated codes such as COR-STAT1202 included); numbers, as digits or words, only for the weekly hours in the evidence. Cached text is re-checked on every hit. Anything else falls back to the matching engine's own explanation, labelled "Standard explanation".
- **Limitations:** free-tier prompts may be used by Google and read by human reviewers, and users must be 18 or over; the daily limit is in memory and resets on restart; the prompt is English-only; checks cannot catch every inaccurate sentence, so the panel says the text may be inaccurate; wording can change when the model changes.
- **Development assistance:** Claude helped design, plan, implement, review and test E1: the backend `matchexplanation` package, Team A's `MatchService.evaluate`, the frontend panel, and the planning documents under `docs/superpowers/`. Charlize reviewed the design, the plan and the review findings; authors remain responsible for explaining every line.

### Live examples from the running application (18 October)

Recorded through `GET /api/matches/{id}/explanation` on a disposable database with the demo seed,
signed in as the presenter account, prompt v5 before the zero-hours wording rule was added.

| Card | Score breakdown | Result | Accuracy note |
| --- | --- | --- | --- |
| GOOD, IS442 search, total 0.6, 0 shared hours | course 1.0, availability 0.0, meeting mode 1.0, study goals 0.0, group preference 1.0 | **AI:** "You and this student match best on course focus for IS442 Object Oriented Programming, meeting mode, and group preference, which carry the most weight for this match. Your weakest shared area is availability, with about 0 hours per week in common. You do not share study goals." | Correct strengths and weakness. "About 0 hours" read awkwardly; the prompt now says to describe zero hours as no shared free time |
| FAIR, concept-review search, total 0.4, 0 shared hours | course 0.0, availability 0.0, meeting mode 1.0, study goals 1.0, group preference 1.0 | **AI:** "Meeting mode is your strongest area among the heavily weighted criteria, and you also match well on study goals and group preference. In contrast, course alignment for IS442 Object Oriented Programming and availability are your weakest areas, with about 0 hours per week in common." | Correct. IS442 is the presenter's saved target course, which a goal-only search scores against |
| STRONG, IS442 search, total 0.95, 4 shared hours | course 1.0, availability 1.0, meeting mode 1.0, study goals 0.5, group preference 1.0 | **Standard** (first attempt timed out at the then 3s timeout; the timeout is back to 5s) | Still to be recorded after the daily quota resets |
| STRONG, COR-STAT1202 search, total 0.85, 2 shared hours | course 1.0, availability 0.5, meeting mode 1.0, study goals 1.0, group preference 1.0 | **Standard** (HTTP 429; the cooldown then skipped further calls and answered in 0.4s) | Still to be recorded. Two direct v5 replies for this course passed the checks and are pinned in `ExplanationOutputValidatorTest` |

The current prompt is `backend/src/main/resources/prompts/match-explanation-v5.txt`. Earlier versions
(v1 to v4) are in the git history; each change of wording bumped the version, which retires
every explanation cached under the old one.
