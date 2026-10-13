# AI usage

7–13 Oct 2026. The 7–8 Oct work was reviewed by Aryan.

- **Planning:** compared the brief and issues; set Team C's scope, contact privacy,
  delete/deactivate behaviour and usage definitions.
- **Backend and frontend (GPT-6.1):** Team C controllers, services, DTOs, migrations,
  screens, shared UI primitives, tests and platform integration.
- **Code review (GPT-6):** found bugs and added fixes with regression tests.
- **Follow-up fixes and UI polish:** navigation, admin dialogs, batched queries,
  responsive views, filters, capacity meters and unsaved-change warnings.
- **Docs and test fixes (Claude):** simplified working notes, wrote coverage/disclosure,
  fixed timing and deactivation bugs, and split platform work into Team B's branch.
- **Team A completion (13 Oct):** reviewed and completed matching, discovery and admin
  configuration, with integration tests, diagrams and documentation.
- **Main audit and fixes (13 Oct):** GPT-6.1-sol helped fix profile/availability races,
  time drafts, group state, links, setup and docs; GPT-6-astra independently reviewed them.
  The protected database rollout and original favicon are recorded in the supporting docs.
- **Room foundation:** helped with draft PR #52's rooms, Pomodoro, presence and audio;
  its lifecycle finding and remaining bonus work stay separate from this PR.
- **Verification:** used regression tests, real browser/API workflows, restart/reseed
  checks and independent review. Details are in [TEAM_C_TESTING.md](TEAM_C_TESTING.md).

Later changes still need cross-team human approval. Each member should confirm their own
retained AI use before submission. Development assistance does not mean E1's AI explanation
feature or the E2 bonus is complete.
