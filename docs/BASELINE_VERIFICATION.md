# Compulsory baseline verification

## Scope

This pass starts from main `fc1f6fb0526726d06c07a8bbf780b5a61c07399c` on
`fix/team-b/baseline-edge-cases`. It completes the remaining compulsory contract and repairs:
login's UTF-8 byte boundary, independently saved profile/week drafts after refresh errors,
and shared contact-number validation/normalization. E1/E2 and draft #52 are excluded.

The project owner accepted Singapore/international contact formats and interpreting the
undefined learning-style wording through the existing defined preferences. These are recorded
team decisions, not claims of professor confirmation. No separate taxonomy/scorer is added.

## Baseline issue closure

| Issue | Delivered behavior / evidence |
| --- | --- |
| #1 Scoring | Five independent scorers, immutable bounded score, unique interval overlap and hand-calculated fixtures |
| #2 Search/API | Current course/goal intent, typed filters/sorts, active eligibility, bounded deterministic results and privacy-safe DTOs; real matching HTTP tests |
| #3 Matching screen | Backend strategy order, precise bars/quality/shared-time view, filters and current request actions; rendered and browser workflows |
| #4 Admin matching | Versioned persisted overrides, enable flags, weights/thresholds, stale-write conflicts, coherent snapshots and restart evidence |
| #5 Foundation/data | Fixed stack, layered services/DTOs, migrations/RLS, repeat-safe 10-course/50-student seed and documented private setup |
| #6 Auth/access | Student registration, JWT role/version/active checks, logout/expiry purge, neutral invalid credentials and UTF-8-boundary regression |
| #7 Own profile/preferences | Full owner-only profile/preferences writes, independent target course, accepted contact grammar and learning-style interpretation; owner drafts recover safely |
| #8 Availability | Complete-week replacement, overlaps/duplicates/adjacency/time validation, invalid draft save blocking and per-account concurrency; empty onboarding remains incomplete |
| #9 Shell/Home | Shared retro primitives/guards, actual dashboard data and correct views, refresh/state/focus coverage |
| #10 Requests/connections | Context, receiver decisions, duplicate/race/state prevention, exactly one connection, disconnect and history |
| #11 Contact privacy | Public/connected DTO boundary; self/active buddy access only; group/admin reads do not bypass it |
| #12 Groups | Browse/create/edit, course/goals/mode/capacity/weekly schedule, applications/leader decisions/removal/closure and last-place concurrency |
| #13 Notifications | Recipient scope, correct links, filtering/read state/unread counts, safe summary and transactional rollback |
| #14 Admin accounts | CRUD/usage, write-only contact replacement, distinct deactivate/reactivate/delete, dependency cleanup, self-removal and revoked-token safety |

A/C issues were already delivered; the intended PR closes the remaining #5–9 after review
and merge. Supporting materials #17–20/#41–46 and selected extras #15/#16 retain their own
scope. Human review, author understanding, presentation and grading/submission acceptance
are separate from implementation completion.

## Focused regressions

| Risk | Reproduced failure | Repair / meaningful checks |
| --- | --- | --- |
| BCrypt UTF-8 boundary | A 72-byte valid Unicode password plus an incorrect 9-byte suffix returned 200 | Login now returns existing 401 `INVALID_CREDENTIALS` before comparison; valid boundary still logs in, usage unchanged on failure, unknown/inactive neutrality and legacy short-password compatibility retained |
| Owner draft lifetime | Save availability, fail the following profile GET, retry: another unsaved name vanished without confirmation | Same owner's editor remains mounted through network/server/catalogue errors; both save directions retain the other draft and discard guard. Permission/purge/owner loss clears it; late writes do not rename another account |
| Contact contract | Arbitrary nonblank text passed writes; UI/server had no format contract | Shared normalization and validation at registration/admin creation, own profile and optional admin replacement; invalid input atomic, canonical self/connected output, admin privacy/omission and repeat-safe synthetic seeding |

The login regression failed the original code (one failure, expected 401, actual 200).
The profile recovery regressions failed before retention (four failures in the focused run)
and pass after the fix. Tests assert user/server outcomes rather than only method shape.

## Automated verification — 14 October 2026

| Check | Result |
| --- | --- |
| Java 21 full backend and disposable PostgreSQL 17 migration/HTTP suite | 380 passed; zero failures/errors/skips |
| Frontend rendered/unit suite | 273 passed across 30 files |
| Frontend lint, TypeScript and production bundle | Passed |
| Documented development backend package | Passed |

Commands from the repository root:

```bash
scripts/test-backend.sh
```

```bash
cd frontend
npm test
npm run lint
npm run build
```

```bash
cd backend
./mvnw -DskipTests package
```

The backend harness generates private credentials and creates/removes disposable databases.
Migration repeat/legacy/preflight tests use separate private targets. Never use destructive
fixtures against shared Supabase. The package command skips tests and is a development step.

The existing CI pipeline remains two jobs with no new services, libraries or browser-test
framework. New coverage is limited to boundary, recovery/privacy, contact write paths and seed
preservation; related fixtures were adapted. Small pure-rule cases remain unit tests, while
ownership/rollback/concurrency/privacy use real HTTP/PostgreSQL. The complete local backend
run took about 35 seconds in this environment; timing is diagnostic, not a CI guarantee.

## Live verification record

The packaged Java 21 app started with `ddl-auto=validate` against an ordered-migration,
private PostgreSQL 17 database. Synthetic identities exercised 42 successful/expected-error
HTTP interactions across auth, matching, requests/accept/decline, notifications, group
application/approval/removal/closure and admin contact/omission/lifecycle/deletion.

- A 72-byte Unicode password logged in; its 81-byte incorrect suffixed input returned 401
  and left `lastLoginAt` unchanged. Invalid registration contact returned 400 without an orphan.
- Singapore input stored with `+65`; international separators normalized. Admin contact input
  remained absent from responses, and omitted contact preserved the existing value.
- Search returned 14 real IS442 results without private contacts. Accept revealed contact;
  group payloads withheld it. Deactivate revoked the old token; reactivate required fresh login
  and restored no connection. Permanent delete rejected the removed account's token.
- In real Chromium, saving availability returned 200. One deliberately intercepted subsequent
  GET503 left an unsaved name visible; retry200 preserved it and the discard guard worked.
- Conversely, saving profile/contact returned 200 while the invalid `12:` time draft survived
  an injected GET503 and retry. Canonical contact reloaded as `+6591234567`.
- Fresh seed produced 10 courses, 50 students/one admin, with all 50 contacts valid. A real Java
  restart with seeding enabled preserved counts and content digests of all 17 application
  tables, including edited profile/week/contact, passwords and relationship history.

Injected 503s are controlled failure tests, not spontaneous server failures. The target was
isolated and synthetic; no shared database deployment or reset is claimed. A clean source snapshot passed locked `npm ci`/build and the documented development
package without test environment variables. Default `spring-boot:run` loaded private ignored
local YAML, validated the migrated schema and completed an admin login. Temporary browsers,
servers and the database were removed; generated local credentials remained outside source.
The final independent baseline/test audit is recorded before publication.

## Independent final audit

The final independent audit reviewed `4ea4b31f8451143bdd875cab5b869a0cbffd95f5` against
the formal brief, latest discussion, role/design material, live #1–14 scope and saved checks.
It found no confirmed actionable code defect. The compulsory A/B/C implementation is complete
under the accepted team contracts. The added tests protect distinct rule, integration and
interaction boundaries; no test removal, dependency, CI job or extra infrastructure was requested.

Publication records must align #7/#41 with the accepted learning-style interpretation without
claiming professor confirmation, and #8 with the established same-day slot contract. Automatic
or exact overnight splitting is not implemented. Local browser evidence is not physical-display
or latest-Figma sign-off. Hosted PR checks, human cross-team review and merge remain separate.
The later verification-record update does not change the reviewed runtime source.

## Delivery gates

This section records the gates at the original baseline audit. Later main source includes
merged E2 and the diagram exports; see the [current coverage/status map](REQUIREMENT_COVERAGE.md#scope-and-evidence-status).

Before publication, the owner reviews the proposed simple [AI usage](AI_USAGE.md) document.
After publication, retain the PR's own hosted CI and cross-team review; use Squash and merge.
The independent final audit checks the original brief, the accepted contracts, baseline
feature coverage and test usefulness as well as this diff.

Remaining project work is selected E1/E2 integration, readable final diagram exports, library
and member contribution/disclosure confirmation, stable integrated demo states, Figma/browser/
projector acceptance, slides/rehearsal, official LMS deadline/receipt and private peer evaluation.
The team's learning-style interpretation remains explicit; later professor-directed changes
must be recorded and implemented rather than represented as an existing confirmation.

## Audit cleanup — 18 October 2026

The cleanup starts from main `9592fd2`. It corrects demo-reset notification cleanup,
narrow weekly-editor containment and touch/keyboard access to matching's shared-hours
details. It also reconciles current documentation with the merged source and adds the
[controlled SVG diagram set](diagrams/README.md). No shared database reset or deployment
was performed by these checks, and the existing AI disclosure was not edited.

| Boundary | Targeted evidence |
| --- | --- |
| Demo reset | The new isolated SQL fixture fails against the original reset because group/buddy session notifications dangle. The fixed reset passes both parent scopes, resource-type ID collision, retained demo-created session and repeat-reset cases; every retained public-table row matches its pre-reset snapshot |
| Migration/database boundary | Existing disposable PostgreSQL 17 harness passes migration preflight/rollback/replay and reset checks; `DatabaseBoundaryTest`: 3 passed, no failures/errors/skips |
| Frontend regressions | 52 tests in 4 focused files pass: heatmap, matching page, weekly editor and dialog. Heatmap tests check same totals in compact table and expandable text, changed data and empty state |
| Narrow layout and controls | 58 Chromium renders: 23 populated route/states at each of 360/390 px, plus profile/group/matching at 768/1024/1101/1440 px. No page exceptions, unexpected API requests, overflow or weekly-editor containment failures |
| Touch/keyboard interactions | Both weekly editors' duplicate/remove/copy/dialog actions and heatmap tap/Enter/Space/focus pass at six widths. Final filter-chip sizing rechecked in 6 additional matching renders; tested button heights are at least 44 px and dialog close targets are 44×44 px |
| Diagram/document checks | Nine deterministic SVGs parse as XML and match their editable source; referenced Java class names resolve. Exports were locally rasterized and visually inspected. API priority defaults now match YAML; E2 rollout, DTO privacy and room locking are described consistently |

At 360 px, the weekly editor and parent both measure 301.62 px and the document is 360 px
wide; at 390 px, both measure 331.62 px and the document is 390 px. The browser pass uses
synthetic HTTP fixtures and validates layout/local interaction. It does not verify database
writes or live external services. Full-suite and real-backend evidence must be recorded
separately against the final combined changes.

The complete cleanup branch subsequently passed the following checks on the same date:

| Check | Observed result |
| --- | --- |
| `scripts/test-backend.sh` with Java 21 and disposable PostgreSQL 17 | 472 passed; zero failures/errors/skips, including fresh/legacy/repeat migration and demo-reset regression checks |
| Frontend `npm test -- --maxWorkers=2` | 320 passed across 37 files |
| Frontend lint, TypeScript and production build | Passed |
| Packaged application against a fresh private migrated database | 15 runtime checks passed: seed, matching, profile/availability, accepted-request contact disclosure, buddy/group sessions and calendar, room/timer, restart preservation, demo reset and reseed |
| Real Chromium with that backend | 10 checks passed: sign-in, saved weekly copy/reload, narrow profile/group/matching/room views, expandable shared-time details, timer/weather controls and saved session detail; no page exceptions |

Reset/reseed retained non-demo accounts and E2 data, removed dangling demo-session
notifications and rebuilt the expected 50-student dataset. A seeded restart preserved every
application table's contents. All browser/runtime fixtures were synthetic; owned servers,
browsers and disposable containers were removed afterwards. The initial local frontend run
under concurrent Java/browser load exceeded asynchronous test timeouts; the unchanged suite
passed with two workers. No timeout increase, test deletion or CI job was added.

Physical iPhone/Safari and Android/Chrome checks remain for music playback/interruption,
background recovery, virtual keyboard, text zoom, clipboard and calendar import. The
existing Outlook acceptance deferral remains. These targeted results do not establish
latest-Figma approval, a physical-projector check, hosting, an LMS deadline or submission.
