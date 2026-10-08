# AI usage

7-8 Oct 2026, reviewed by Aryan.

- **Planning:** compared the branch with the brief and issues #10-14. We set the scope and
  the admin delete/deactivate and usage decisions.
- **Backend and frontend (GPT-6.1):** Team C controllers, services, DTOs, migrations,
  screens, shared UI primitives and tests.
- **Code review (GPT-6):** found three bugs; fixed with regression tests.
- **Follow-up fixes:** navigation after save, admin dialog state, batched list queries.
- **UI polish:** responsive admin view, URL filters, capacity meters, unsaved-changes warnings.
- **Docs (Claude):** trimmed working notes, wrote this file and the coverage table.
- **Test fixes and branch split (Claude):** fixed timing-dependent tests; moved platform
  code to `feat/team-b/platform-foundation` for Team B.
- **Code review and fixes (Claude, 8 Oct):** found deactivation bugs and dead code; fixed
  with regression tests and simplified the exception handling.

9 Oct 2026, room foundation; author walkthrough remains pending.

- **AI-assisted drafting:** immutable timer, private room/presence endpoints, migration,
  retro screen, original audio synthesis and focused regression tests.
- **Scope decisions by Aryan:** existing retro components, original built-in audio,
  a modest E2 foundation, and sessions/calendar/E1 left for teammates.
- **Validation:** local isolated database and frontend checks; results recorded in the
  coverage document. AI assistance and automated checks do not replace author understanding,
  cross-team review or the viva walkthrough. No AI provider is integrated into the product.
- **Final review correction:** leaving switched to preview after the global refresh event,
  allowing an expired heartbeat. Changed participation/cancellation order and added a
  regression for a deleted lease.
