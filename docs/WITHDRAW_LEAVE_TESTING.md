# Withdrawal and leave verification

This guide covers PR #67 on `feat/team-c/withdraw-and-leave`, including the 23 October
review fixes appended after the original implementation at `43ba3b4`. The original commits
and authors are retained. It records local technical evidence, separately from team approval,
shared-database rollout and submission. The [API contract](API_CONTRACT.md) defines the endpoints.

## Confirmed defects and fixes

The original group and profile confirmations stored only an action kind and read the current
group or relationship when confirmed. Navigating from group A to B, switching accounts or
receiving a replacement pending request during polling could reuse the old consent for a
different target. Group management had the same route-lifetime problem for closure/removal.
A focused before-fix run reproduced 12 failures across 13 cases; the unchanged-target refresh
case passed.

Group detail, group management and profile content now have route/account-scoped lifetimes.
Confirmations keep their original target IDs and names, and check that the displayed source
record still matches. Group membership confirmations also compare `joinedAt`, so a readmitted
member has a new tenure requiring new consent. Stale consent is cleared when its source changes;
an unchanged background refresh keeps the dialog open. Losing the student account unmounts
private content and stops its reads. The existing shared dialog and resource primitives remain
unchanged.

Withdrawal copy now accurately states that a cancelled buddy request remains in the recipient's
Past requests and can no longer be accepted. It does not promise deletion of retained history.
The request state-machine Javadocs also include withdrawal. A group-page test fixture now returns
a session array for its session endpoint instead of returning a group object to every GET.

## Repeatable checks

With Java 21, Docker, PostgreSQL CLI tools and Node/npm available, run from the repository root:

```bash
./scripts/test-backend.sh
cd frontend
npm ci
npm test -- --maxWorkers=1
npm run lint
npm run build
```

The backend script creates and removes its own private PostgreSQL 17 container and temporary
credentials. It runs fresh/repeated/legacy migration checks, including retained `WITHDRAWN` and
new notification rows, legacy withdrawal backfill and final validated enum constraints. These
destructive fixtures must never target the shared database. No live explanation-provider call
or new dependency is needed.

| Area | Regression evidence |
| --- | --- |
| Route/account consent lifetime | `ConfirmationLifetime.test.tsx`: group leave/withdraw, profile decline/withdraw/disconnect and group-management closure dismiss after route change; account changes and logout dismiss dialogs |
| Same-route replacement | Pending request/application replacement and new membership tenure invalidate old consent; unchanged refresh preserves the exact dialog and original request ID |
| Existing feature paths | Group, connection and profile suites cover successful actions, role visibility, stale server conflicts and retry behavior |
| HTTP roles and state machines | `LifecycleHttpTest`: sender-only cancellation; applicant-only withdrawal; new requests after withdrawal; non-member/leader/closed-group leave conflicts; anonymous 401 and administrator 403 without mutation |
| Leave access revocation | Real HTTP checks reject former-member room read/join/heartbeat, group sessions, session detail and calendar export; their own session list excludes it; remaining-member access and the scheduled session are retained |
| Atomic failure | A rejected leave-notification write rolls back membership deletion and room-presence revocation, creates no leave event and preserves authorized room access |

The final standalone run passed all 601 backend tests with zero failures/errors/skips on Java 21
and disposable PostgreSQL 17.11, including six lifecycle HTTP tests. All 364 frontend tests
across 39 files passed with one worker, including 19 confirmation-lifetime regressions.
Lint passed without warnings and the production build passed. Browser verification of the
combined Help/Achievements/lifecycle branches is a separate integration layer; isolated
component tests are not claims of real-backend browser acceptance.

## Coordinated migration gate

This PR adds `20261023090000_withdraw_and_leave.sql` and adjusts the replayed session migration's
intermediate notification constraint. The final migration permits `WITHDRAWN`,
`GROUP_JOIN_REQUEST_WITHDRAWN` and `GROUP_MEMBER_LEFT`, and converts identifiable earlier
deactivation withdrawals without changing genuine leader rejections. Replay is checked on
populated disposable fixtures, not inferred from compilation.

The shared Supabase database has not been changed by these checks. A database owner must
coordinate the versioned migration and compatible backend release before the new flows are
enabled, verify the backfill and final validated enum constraints, and retain the normal backup
and rollback evidence. An older backend cannot interpret the new `WITHDRAWN` application state,
so the controlled cutover is:

1. Stop the old backend instances or hold application traffic so they cannot read backfilled rows.
2. Run the versioned migration through `scripts/migrate-database.sh` with its existing database
   advisory lock and check the backfill plus final validated constraints.
3. Start the compatible new backend and verify schema validation and the lifecycle flows before
   reopening traffic.

Do not restart an older backend against the backfilled enum values as a rollback shortcut.
Passing local tests or merging the PR does not satisfy this deployment gate.

Cross-team approval, both authors' explanation of retained code, physical-device/projector
acceptance and final submission remain separate human gates.
