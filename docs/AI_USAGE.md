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
