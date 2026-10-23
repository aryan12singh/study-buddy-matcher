# Achievements verification

This guide covers the Achievements extension and its 23 October refinements on
`feat/team-c/achievements`, after merging main revision `03e6e6c`. It records source
and local verification, separately from cross-team approval, hosted acceptance or submission.
The [API contract](API_CONTRACT.md#achievementsdto) is the detailed metric policy.

## Scope and ownership

The implementation derives six private badge standings and a planned-session grove from
existing records. It adds no schema, award ledger, attendance or completed-focus tracking.
Current live counts may fall; retained sent requests and created groups remain history
while their records exist. The longest run is recomputed from currently eligible schedules.

Achievement code owns the controller, service, assembler, typed configuration, pure badge
and streak rules, and a read-only schedule projection. The projection retrieves schedule
fields and participation start times together, avoiding per-session entity loading.
`GroupMembershipRepository` adds an achievement-specific open-membership count without
changing the existing administrator usage count.

The Team B profile addition is limited to a conditional, owner-keyed
`OwnAchievementSummary`: student role, route id, returned profile id and `SELF` relationship
must all agree before it mounts or fetches. Other profiles and administrator accounts do
not request achievements. Existing `useResource` supplies polling, purge, abort and stale
response handling. No shared authentication, security, routing or client behavior changes.

## Repeatable automated checks

Use Java 21 and Docker with PostgreSQL 17 available. From the repository root:

```bash
./scripts/test-backend.sh
cd frontend
npm ci
npm test -- --maxWorkers=1
npm run lint
npm run build
```

The canonical backend script creates a private disposable PostgreSQL 17 container, runs
fresh/repeated/legacy migration checks and schema verification, then the complete test suite.
It removes its container and temporary credentials on exit. Never substitute the shared
project database for this destructive test fixture. No live explanation-provider call is
needed for these checks.

| Area | Regression evidence |
| --- | --- |
| Tiers and hints | `BadgeEvaluatorTest`: locked/next/Gold tiers; Grove Keeper states the full consecutive-run target rather than promising that one next week can extend an already broken best run |
| Summary and cap | `AchievementAssemblerTest`: all-Gold level cap; non-divisible final level; level step larger than all available points |
| Configuration | `AchievementPropertiesTest`: zero/negative/unsorted thresholds and zero level step rejected before counting/division |
| Streak policy | `WeeklyStreakCalculatorTest`: empty, duplicate-week, consecutive, gap, previous-week grace, two-week expiry, timezone/Monday boundary, later-this-week and future-week rules |
| Service | `AchievementServiceTest`: exact metric sources, current-tier points, tenure boundary, downward recomputation and student guard before repository reads |
| Real HTTP/JPA | `AchievementHttpTest`: anonymous 401/admin 403, principal-only `/me` including an ignored `studentId` selector, separate accounts and public-profile payloads; active/ended connections; retained request/group history; open/non-led memberships; GROUP-only and combined GROUP/BUDDY schedules; exact current tenure; removal/readmission/cancellation; closed groups and inactive buddies |
| Page | `AchievementsPage.test.tsx`: allowlisted destinations, current/next tier and labelled progress, precise planning explanation, seed and empty actions, bounded tree list, Gold cap, loading/retry/purged failure, aborted/obsolete account responses and logout/admin guard |
| Own profile | `StudentProfilePage.test.tsx`: conditional owner fetch; no other/admin fetch; independent failure/retry; abort plus late response after navigating away; account change/logout immediately hides the summary |

The final 23 October run passed all 620 backend tests with zero failures/errors/skips,
including 22 achievement unit tests and five achievement HTTP tests, on disposable
PostgreSQL 17.11 with Java 21. All 357 frontend tests across 39 files passed with one
worker; lint and the production build passed. The migration harness passed as part of
the backend command. A mock rendering check does not establish backend behavior; the
HTTP tests above use the real application/security/JPA stack against PostgreSQL.

## Browser rendering and keyboard pass

The 23 October Chromium rendering pass used a local Vite server and synthetic intercepted
API responses, with screenshots inspected at desktop 1280 px and narrow widths 390/320 px.
It checked progressed standings at all three widths, an empty seed at 320 px, all Gold at
390 px and the own-profile summary at 390 px. All six layouts had document width equal
to viewport width and no badge card outside the viewport; no uncaught page exception occurred.
The compact profile summary changes to three short rows on narrow screens.

Keyboard Enter toggled the native metric-help disclosure; Tab reached the next badge action.
With reduced motion enabled, grove and medallion animation names were `none`.
The empty-grove session action navigated to `/sessions`. Opening another student's profile
rendered no private summary and made zero achievement requests. These are rendering and
interaction checks with mock responses; combined browser checks against a running real
backend are a separate verification layer.

## Demo and review checks

1. As a new student, open Achievements: every badge is locked, the grove is a seed and its
   buddy/session actions are usable. Open metric help and read the planning/attendance distinction.
2. Follow badge actions to matching, connected buddies, group browsing, new group and sessions.
   Session creation still follows the existing parent membership and manager permissions.
3. Join an open group or accept a buddy, then schedule a recurring session. Planner counts
   individual eligible occurrences. Streaks count distinct Monday-based weeks only through
   the current week; a plan later this week counts now, and next week waits.
4. Remove a membership, cancel or edit a schedule, or disconnect. Refresh and confirm that
   current live counts and affected runs recompute. Founder still includes closed retained
   groups, and First Move still includes retained requests in any status.
5. Readmit a member to a group with old sessions: older occurrences before the new membership
   do not count. The same temporal boundary applies to an exact buddy connection.
6. Open your own profile and its full-page link. Open another profile, switch accounts and
   log out during a delayed request: the old owner's standing must not appear.
7. At all Gold, counts can exceed the Gold thresholds, progress stays capped and no next
   level or tier is promised. Configured level steps must still have an attainable final step.

Independent source review and final team walkthrough remain separate from these automated
checks. Latest-Figma comparison, complete contrast/browser/screen-reader acceptance,
physical-device/projector readability and LMS submission need their own evidence.

An exploratory unsupported `/api/achievements/{id}` request exposed the existing global
unknown-route handler's generic 500 response. It returned no achievement data; that path
is not implemented. The global handler is unchanged here. Principal isolation is tested
through the sole `/me` route instead of claiming an unsupported path has a 404 contract.
