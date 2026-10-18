# Team A testing and demo guide

Scope: matching algorithm, API, matching page and admin matching configuration
([#1](https://github.com/aryan12singh/study-buddy-matcher/issues/1),
[#2](https://github.com/aryan12singh/study-buddy-matcher/issues/2),
[#3](https://github.com/aryan12singh/study-buddy-matcher/issues/3),
[#4](https://github.com/aryan12singh/study-buddy-matcher/issues/4)).
Use [REQUIREMENT_COVERAGE.md](REQUIREMENT_COVERAGE.md) for the source/test mapping and
[API_CONTRACT.md](API_CONTRACT.md) for query, DTO, configuration and error details.
The verification record below lists completed local checks. The procedures remain repeatable;
the PR records hosted checks for the delivered commit. Reviewer approval and presentation
hardware acceptance are separate gates.

## Automated checks

Java 21, Node 22, Docker, `psql` and OpenSSL are prerequisites. From repository root:

```bash
# Pure/unit backend tests without shared database access
cd backend
./mvnw test -Dtest='!com.studybuddy.integration.**,!StudyBuddyApplicationTests' \
  -Dsurefire.failIfNoSpecifiedTests=false
cd ..

# All tests, real PostgreSQL HTTP tests, context load and migrations
scripts/test-backend.sh

# Rendered React interactions, type/build and lint
cd frontend
npm ci
npm test
npm run lint
npm run build
```

The backend script starts a disposable PostgreSQL 17 container with generated credentials,
waits for the published TCP endpoint to accept SQL, exercises ordered migrations, runs the
full Maven suite and removes the container. It never points at the shared Supabase database.
The TCP readiness check also covers the official image's temporary initialization server;
a ready internal socket alone is not enough before host-side `psql` connects.

Useful focused suites while developing:

```bash
cd backend
./mvnw test -Dtest='com.studybuddy.matching.**' \
  -Dsurefire.failIfNoSpecifiedTests=false
cd ../frontend
npm test -- src/features/matching src/features/home
```

## Scoring fixtures to explain in the viva

Scores describe preference compatibility, not whether students will succeed together.
The following calculation uses Balanced default weights and a course/goal selected in the
current search:

| Criterion | Fixture | Score | Weight | Contribution |
| --- | --- | ---: | ---: | ---: |
| Course | Candidate targets the selected course | 1 | 0.30 | 0.30 |
| Availability | Each student has 6 unique free hours, of which 3 are shared | 0.5 | 0.30 | 0.15 |
| Study mode | Caller online; candidate either | 1 | 0.20 | 0.20 |
| Goal | Candidate has the selected goal | 1 | 0.10 | 0.10 |
| Group size | Candidate has not supplied a range (unknown) | 0 | 0.10 | 0 |
| Total | Sum of contributions | | 1.00 | **0.75** |

With default quality cut-offs this is a Strong match. Exact total 0.75 drives ranking,
threshold and quality; the screen formats it as 75% compatibility. The missing group range
is unknown: it scores the configured unknown value (0), the card shows "Not shared", and a
specific group-size filter cannot claim it is compatible. Availability divides by the smaller
timetable, so the score is the same from either student's side.

`AppendixAMatchingTest` reproduces the brief's Appendix A table with the real scorers and
asserts the 2-hour overlap and a 0.85 Strong total under Balanced weights.

For the Appendix A case, the caller's two 2-hour blocks total 4 hours, the one shared block
is 2 hours and availability is 2/4 = 0.5. Adding a duplicate shared block or a nested block
must keep shared time at 2 hours. Two blocks that only meet at 21:00 share 0 minutes.
Weekly slots recur in SGT; UTC timestamp handling does not alter these local preferences.

Strategy fixtures deliberately put a higher priority band against a larger weighted total.
Availability-first ranks by shared-hour bands (3 h or more / 1 to 3 h / under 1 h) and
Course-first by course bands at 1 and `takes-course-score` (wants a buddy for it / takes it /
neither) before the total; an unknown priority criterion is always in the lowest band, and
lowering `takes-course-score` must not merge "takes it" into "neither";
within a band the total decides, so the displayed score never increases down a band.
Disabling the priority criterion must remove that comparison. Equal strategy results use
more shared hours, then case-insensitive name, then ID.
MOST_SHARED_HOURS and SAME_YEAR_FIRST add their comparison before this existing chain.

## Acceptance scenarios against a local demo

Use the existing seeded synthetic accounts on a disposable/local demonstration database.
Do not reset or seed a shared database solely for review. The opt-in seeder needs private
password values from the runtime environment or gitignored local configuration; no real
password is recorded here. The seed minimum (10 courses/50 students) remains Team B's
shared foundation, reused by matching.

### 1. First use and live matching

1. Sign in as a student. If no saved target or goal exists, Home explains the next step and
   links to preferences; it must not issue `GET /api/matches` without a context.
2. Open Find buddies. Select only a course and search; repeat with only a study goal.
   Selecting a course different from the saved target must score the selected course.
3. Confirm names/programme/year, relevant course/goals, total/quality and criterion bars
   match the API payload. Check the shared-hours total against the heatmap. The main API
   still returns a deterministic explanation, but the main card does not render it;
   provider-assisted wording belongs to separate PR #61.
4. With a saved target or goal, Home displays up to three results from the same endpoint,
   configuration revision and server ranking. Open the link to preserve that search context.
5. Confirm incomplete preferences have clear setup guidance and show as "Not shared"; neither
   view fabricates matches or requires an AI provider to work.

### 2. Filters, sorts and limits

1. Add mode, group preference, goal, minimum shared hours and minimum quality to a valid
   course/goal context. Each active selection is readable and removable.
2. Remove each filter and reset. Reset must return to the documented initial/first-use
   state without creating an invalid unqualified backend request.
3. Try Best match, Most shared hours and Same year first. The screen order must match the
   returned order; different quality badges must never regroup the cards.
4. Inspect a deliberately tied fixture with duplicate/case-varied names: final ID ordering
   must be stable. Set a small valid result limit: the highest-ranked students remain,
   `totalMatches` still describes the filtered set before truncation.
5. Use incompatible filters or a high threshold to produce no matches. Show the empty
   explanation/reset action; a 200 empty result must not display as an API error.

### 3. Eligibility, privacy and requests

1. The current student, inactive/deleted accounts and active buddies must be absent.
   Pending incoming/outgoing requests remain with the proper disabled/request action.
2. Open a stranger's profile and inspect both discovery/profile network responses: no
   contact field is present. Discovery also has no email, credentials, tokens or entities.
3. Send an optional-message request. The card becomes outgoing pending as soon as the POST
   succeeds, including while a GET refresh is still pending. A failed POST permits retry.
4. Accept through the existing recipient Connections screen. The next discovery refresh
   removes the connected candidate; the connected profile may now reveal contact through
   Team C's backend rule. Ending a connection removes profile contact access again.
5. An earlier search reply finishing during a later send must not undo the acknowledged
   pending state. A post-send refresh showing a decline/cancellation may return to Stranger.

### 4. Admin configuration, concurrency and restart

1. Sign in as admin, open Matching settings and note the defaults/override indicator and
   revision. Switch among all strategies; each retains its own full five-weight set.
2. Disable the active priority criterion, rebalance enabled weights and save. Search again
   as a student to confirm the total-only fallback and disabled breakdown. Re-enable it and
   demonstrate its priority with the opposing-total fixture.
3. Save precise fractional weights such as 0.004/0.996 with the other weights zero. Reload
   and confirm they remain exact. Set a valid threshold and confirm it filters lower totals.
4. Attempt negative weights, zero total, an invalid sum, no enabled criteria and an invalid
   threshold. UI/API must reject them and preserve the last valid saved settings.
5. In two admin sessions load the same revision, save one, then try saving/restoring the
   stale second one. It must receive a conflict and offer reload, not overwrite the first.
6. Stop/restart the backend using the same local database; reload settings and repeat the
   search. Stored overrides/revision must remain. Explicit Restore defaults confirms first,
   clears overrides, raises the revision and returns the current external startup defaults.
7. Student and unauthenticated GET/PUT/restore attempts must be rejected. Concurrent save
   and search responses must each use a complete old or new configuration/version, never a
   mixture. The HTTP suite automates these boundaries in real PostgreSQL.

### 5. Failure states and presentation

1. Exercise initial loading, no-result, API failure and Retry for matching/Home/config.
   A failure in one Home section must not hide the other working sections.
2. Use the keyboard for every filter, sort, reset, card link, request dialog, config input,
   save and restore confirmation. Labels must identify controls; errors stay near fields.
3. Check the existing retro window shell on the presentation display and a narrow viewport.
   Body text/score bars remain readable and focus is visible; avoid clipped action controls.
4. Failed config saves preserve the unsaved form, clearly distinguish it from the active
   saved revision and permit correction/reload. Dirty edits must not be silently discarded.

## Integration changes to coordinate

- `application.yml` adds typed enabled-criteria, minimum-score and result-limit defaults.
  Existing defaults/override precedence is documented in README's configuration table.
- `20261013090000_complete_matching_configuration.sql` adds singleton version/threshold/
  override state and enabled-criteria storage. Apply ordered migrations before running a
  fresh database; the script validates repeatability and backend-only access rules.
- Home's existing matching card consumes the same endpoint using the configured default bound,
  then displays the first three returned results (or fewer when the configured bound is smaller).
  Team B's profile and dashboard contracts remain the integration source for saved context.
- `scripts/test-backend.sh` adds published-TCP readiness before host SQL during disposable
  verification. No new dependency, production service or shared database write is required.

## Verification record - 13 October 2026

The following checks passed locally against the implemented PR #54 scope. Backend tests
and joined browser flows used disposable/local PostgreSQL and synthetic students; they did
not mutate the shared Supabase project. Hosted CI for the final delivered commit is recorded
separately on the PR.

| Check | Verified result |
| --- | --- |
| Full backend, `scripts/test-backend.sh -o` | 364 tests passed, 0 failures/errors/skips |
| Matching-focused backend tests | 101 passed |
| Matching HTTP integration | 11 passed against real PostgreSQL |
| Configuration isolation regression | Paused search between the real settings and weights queries while another admin saved; deliberately weakening isolation produced the expected mixed-snapshot assertion failure, then production isolation was restored |
| Database and packaging | Legacy upgrade, failed-preflight rollback, repeated and fresh migrations passed; application JAR packaged |
| Frontend, `npm test` | 215 tests passed across 26 files |
| Frontend lint and production build | `npm run lint` and `npm run build` passed |

Real Chromium was connected to the running Java backend for these joined workflows:

- Home preview, course-only and goal-only discovery, all three sorts, filter removal/reset,
  public-profile navigation and return with search context preserved.
- Optional-message request with matching course context; confirmed pending action
  disabled. Actual recipient acceptance revealed contact only through the connected profile,
  excluded the connection from Home/discovery, and disconnect readmitted the student.
- Admin invalid threshold and no-enabled-criteria validation blocked submission. Switching
  Course-first changed both Home and matching order: a Fair Jamie ranked ahead of a Strong
  Alex because course priority preceded total. Disabling that priority and applying the
  threshold left only Alex, proving scoring and eligibility use the saved configuration.
- Restore returned current defaults with version 3. A real stale save returned 409 and kept
  the unsaved `0.004` edit; confirmed reload followed by restore reached version 5.
- Stopping and restarting the complete Java process preserved the exact saved configuration
  JSON, including fractional weights, enabled set, threshold and version 1, and preserved
  the exact matching response. This exercised persistence beyond a service/context refresh.
- With the backend stopped, matching showed a clear error and purged cards. Restarting it
  and choosing Try again restored the response, without leaving stale cards actionable.
- Desktop 1440-pixel and 768-pixel layouts were inspected without horizontal overflow.
  Keyboard typeahead, Tab and Space operated sort, filter and goal controls.

This is local browser and process-restart evidence, not a claim of checking a physical
projector, completing a viva, receiving human review approval or merging the PR.

## What remains a human/project gate

Cross-team review and the authors' line-by-line/viva walkthrough are required by AGENTS.md;
only the reviewers/authors can complete them. PR #54 is not merged by this test guide.
A final presentation/demo can validate SMU Wi-Fi and projector conditions. Learning-style
wording in the objective still needs the professor's interpretation under #41/#7; no sixth
criterion or invented categories are claimed. E1, E2 and final coursework deliverables remain
separate issues and do not block exercising the deterministic M1 matching workflow.
