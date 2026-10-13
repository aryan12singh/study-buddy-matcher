# AI usage

This records known retained assistance through 13 October 2026 for coursework disclosure.
It covers planning, implementation, tests, independent reviews and documentation. The team
remains responsible for the submitted design/code and must be able to explain it in the viva.
The repository's rule against attribution trailers in commits/PRs does not remove the formal
disclosure requirement in supporting materials and the presentation.

## Retained work record

| Period / scope | Assistance retained | Evidence and review limit |
| --- | --- | --- |
| 7–8 Oct, Team C planning | Compared the brief/discussion and #10-14 scope; considered delete versus deactivate, contact privacy and usage definitions | Existing record says reviewed by Aryan; decisions are in `DESIGN_DECISIONS.md`. This does not assert every later change received human review |
| 7–8 Oct, Team C backend/frontend, GPT-6.1 | Controllers, services, DTOs, migrations, screens, shared UI primitives and tests | Requirement/DTO/transaction maps and automated tests; human authors remain responsible for retained code |
| 7–8 Oct, independent review, GPT-6 | Identified three defects; fixes included regression tests | Original disclosure retained; passing automated checks are separate from teammate approval |
| 7–8 Oct, follow-up/UI work | Navigation after save, admin dialog state, batched list queries, responsive admin presentation, URL filters, capacity meters and unsaved-change warnings | Retained Team C source/tests and later merged feature/polish PRs; exact model for each item was not preserved in the original record |
| 7–8 Oct, documentation/test fixes/platform split, Claude | Trimmed working notes; wrote the earlier disclosure/coverage map; fixed timing-dependent tests; separated platform code onto `feat/team-b/platform-foundation` for Team B | This records cross-team foundation work rather than assigning unrecorded individual contributions |
| 8 Oct, review/fixes, Claude | Found deactivation defects/dead code; added regression fixes and simplified exception handling | Original disclosure retained; lifecycle/integration regressions provide technical evidence |
| Subsequent Team C completion, merged by 13 Oct | AI help with main feature completion/platform integration and later UI/backend polish, including PRs #48/#49/#50 | Merged-main audit at `b33f286` includes these PRs. Per-change model/human review records are not inferred from merge status |
| PR #50 follow-up, completed by 13 Oct | Navigation and frontend/backend matching-config compatibility fixes, with regression verification | Fixes were tested and merged before the main audit; this is distinct from later newly reproduced defects |
| 13 Oct, Team A PR #54 review and #1-4 completion | Four-defect review followed by strategy-order/configuration-precision/snapshot/request-state fixes; full matching/search/admin-config scope, frontend integration, tests, diagrams and setup/rollout documentation | `TEAM_A_TESTING.md`, Team A diagrams, PR #54 and current matching source. No professor or unrecorded teammate acceptance is claimed |
| E2 PR #52 draft, separate from merged main | AI help with the collaborative-room/Pomodoro/presence/audio foundation and review of its membership-removal/readmission lease defect | Draft scope only. This completion branch does not implement E2; the known lease regression and full room/session/calendar acceptance remain pending |
| 13 Oct, merged-main independent audit | GPT-6-astra at medium reviewed the backend; GPT-6.1-sol reviewed frontend and requirements/deliverables. Work included independent reproductions of availability concurrency, invalid-time drafts, navigation/setup gaps and a bounded recheck of the separate E2 lease issue | Audited SHA `b33f286887a090cd9b765886263c0590a13a6acf`; 365 backend and 229 frontend baseline tests passed, with local native-browser/Java and hosted CI checks. Additional failing audit assertions revealed missing coverage rather than changing those baseline results |
| 13 Oct, compulsory completion branch | GPT-6.1-sol at max assisted the confirmed backend/frontend/setup/docs fixes and their regression checks, original pixel-book favicon/unused-sprite cleanup, and this disclosure/platform/coverage handoff | `fix/team-c/merged-main-completion`; final verification and exact scope are recorded in `TEAM_C_TESTING.md`. An independent final review requested with GPT-6-astra at medium found no actionable runtime findings; this remains separate from human approval |

Known merged PR references:
[#48](https://github.com/aryan12singh/study-buddy-matcher/pull/48),
[#49](https://github.com/aryan12singh/study-buddy-matcher/pull/49),
[#50](https://github.com/aryan12singh/study-buddy-matcher/pull/50),
[#54](https://github.com/aryan12singh/study-buddy-matcher/pull/54).
The separate room foundation is [draft #52](https://github.com/aryan12singh/study-buddy-matcher/pull/52).

## How the assistance was checked

The retained workflow used the supplied brief/discussion/design materials, repository code,
issue scope, test results and independent audit evidence. Scoring fixtures, state transitions,
privacy DTOs, PostgreSQL concurrency/integrity behavior and rendered UI interactions were
checked with the automated suites and documented live scenarios. Reproduced review findings
were fixed with focused regressions; a green existing suite alone was not taken as proof that
all behaviors were covered.

The [Team A test record](TEAM_A_TESTING.md), [compulsory integration record](TEAM_C_TESTING.md),
[coverage map](REQUIREMENT_COVERAGE.md) and [database rollout record](DATABASE_OPERATIONS.md)
separate implementation, local tests, hosted CI, deployment and human acceptance. Independent
AI review is technical evidence; it is not the cross-team human review or author walkthrough
required by AGENTS.md. No additional human sign-off is invented in this disclosure.

## Application AI and submission completion

The compulsory application currently returns deterministic match explanations. E1 (#15), an
application AI-explanation integration, remains selected-extra scope and is not implemented
on merged main by this completion work. Its eventual record must identify the actual provider,
allowed input fields, data handling, fallback behavior, configuration/cache versioning, usage
limits, tests and human review. Development assistance does not establish that this product
feature has shipped.

Each member must confirm or add their own retained AI use and actual human review before
submission. This log is the known retained record, not proof that every member used the same
tools or that all possible use has already been disclosed. Keep credentials/private contact
data out of prompts and published examples; use synthetic demo data for evidence. Carry the
confirmed disclosure into the presentation and final supporting materials.
