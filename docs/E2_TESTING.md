# E2 testing and acceptance

This guide verifies PR [#52](https://github.com/aryan12singh/study-buddy-matcher/pull/52)
against [E2 #16](https://github.com/aryan12singh/study-buddy-matcher/issues/16).
Its contract is in [API_CONTRACT.md](API_CONTRACT.md); design limits are in
[DESIGN_DECISIONS.md](DESIGN_DECISIONS.md#collaborative-study-experience-e2-16).
Baseline evidence in [BASELINE_VERIFICATION.md](BASELINE_VERIFICATION.md) remains
historical baseline evidence, not proof of these new workflows.

## Automated checks

From the repository root:

```bash
scripts/test-backend.sh
```

From `frontend/`:

```bash
npm test
npm run lint
npm run build
```

The backend harness creates only disposable PostgreSQL, applies the ordered SQL,
verifies migration repetition/adoption/rollback and browser-role denial, then runs
the application tests. It does not use the local runtime configuration to select
a shared database. The weather test profile disables live network requests.

Keep tests at meaningful boundaries:

| Boundary | Essential cases |
| --- | --- |
| Immutable timer | Start/pause/resume/reset transitions; elapsed phase changes; long gaps and restart reconstruction |
| Room HTTP/database | Membership/active-account/role denial, version conflicts, last-slot concurrency, multiple tabs, expiry, remove/readmit/replacement capacity and closure/deactivation cleanup |
| Session schedule/export | Whole-minute/date/duration/occurrence bounds; DST gap/fold rejection; weekly wall-time preservation; stable UID/revision/cancellation, UTC times, escaping and UTF-8 folding |
| Session HTTP/database | Group leader versus member; buddy participants versus strangers/admins; parent-end revocation; stale writes; transactional notifications; private calendar/availability reads |
| Civil-time round trip | Personal/group weekly `10:00` stays `10:00` in SQL/API under Singapore and UTC JVM defaults; session local civil fields and generated UTC instants remain distinct |
| Frontend interaction | Empty/loading/error/retry; invalid/dirty form guards; recoverable actions; separate audio controls/silent fallback; current sessions in Home and agendas |
| Weather | Fixed-condition mapping, timeout/cache/stale/unavailable behavior; original selectable fallback and reduced motion; no account/device-location payload |

Use the existing pipeline and dependencies. A private real-browser walkthrough
provides integration evidence that mocks cannot; neither it nor a green suite
automatically verifies author understanding, latest Figma or external calendar clients.

## End-to-end walkthrough

Use disposable seeded accounts and an open group with a leader and at least two
accepted members. Keep a separate connected buddy pair and a stranger/admin.
Do not put private passwords or tokens in screenshots, evidence or PR comments.

1. In two authenticated browsers, join the same group room. The leader assigns
   host/co-host, starts/pauses/resumes/resets, changes idle durations and adjusts
   capacity. Verify delegated roles cannot reassign roles. Record the observed
   countdown difference, configured poll period and network delay; do not claim
   exact tick or audio-position synchronization.
2. Refresh, disconnect/reconnect and leave one of several tabs. Confirm durable
   timer/audio state, one-student capacity counting, lease expiry and explicit
   rejoin. Remove a member, fill the freed room place with another member, then
   readmit the removed member: old heartbeat must fail and fresh join must respect
   the participant limit. Close/deactivate and confirm lost access and stopped audio.
3. Enable audio intentionally in each browser. Play music and ambience together,
   switch ambience, pause either layer and change each local volume/mute independently.
   Verify shared selection and local sound controls. Force unsupported/failed audio,
   navigate away and revoke access; the room stays usable and audio stops.
4. Create a dated group series with timezone, agenda and location, then edit and
   cancel it. Confirm the agenda and Home read actual saved sessions and affected
   recipients receive correct change notices. Repeat for the connected buddy pair.
   Members/strangers/admins must receive the appropriate management/access denial.
5. Save compatible weekly availability and check mutual suggestions. Clear one
   participant's schedule: suggestions become empty, and manual scheduling remains
   available. Verify group suggestions use current accepted active membership.
6. Download a single and weekly calendar snapshot, including a valid DST-crossing
   series. Compare every exported UTC start/end to the API occurrence list. Attempt
   exports as a stranger, after membership removal and after ending the connection.
   Inspect the file for private contacts/emails/tokens and malformed text injection.
7. Import those files into separate test calendars in Google Calendar, Apple
   Calendar and Outlook. Check event count, displayed local times, title/agenda/
   location, DST transition and cancellation/re-import behavior. Retain actual
   client/version/timezone observations; parsing is not client-import evidence.
8. Select each pixel weather theme/day-night mode. In automatic mode verify live,
   stale and unavailable labels and source attribution; disconnect the provider and
   continue using manual fallback. Reduced motion must stop scene animation, and
   timer/audio/session actions must remain usable throughout.

## Verification record

The completion source passed the checks below on 14 October 2026. Final hosted CI and
independent review must be recorded against the final published commit.

| Gate | Current completion evidence |
| --- | --- |
| Backend suite and migration/access checks | 437 passed, zero failures/errors/skips; fresh, legacy and populated repeat migrations/access checks passed |
| Frontend suite, lint, type check and production build | 305 passed across 34 files; lint/type/build passed (existing lint and bundle advisories recorded) |
| Two-browser timer, host and reconnect; recorded tolerance | 23 real-browser checks passed against Java/disposable PostgreSQL; see observations below |
| Layered audio, failure fallback and exit/revocation | 23 real-browser checks passed against Java/disposable PostgreSQL; see observations below |
| Group/buddy sessions, suggestions, Home/agendas and notifications | 23 real-browser checks passed against Java/disposable PostgreSQL; see observations below |
| Calendar single/weekly/DST/privacy checks | Real single, 3-week London DST and cancelled snapshots downloaded; dates/privacy checked |
| Google Calendar import | Actual Google web import: single 1/1, weekly 3/3; Singapore display 17:00 then 18:00/18:00 across London DST. Cancelled snapshot imported 3/3; earlier weekly events no longer appeared in search |
| Apple Calendar import | Actual macOS Calendar import: single and all three weekly occurrences verified in search; correct Singapore DST display. Re-import marked existing weekly events Cancelled |
| Outlook import | Not verified: school account requires Authenticator approval; owner elected to leave this acceptance check pending |
| Weather provider/fallback and reduced motion | Authenticated actual Open-Meteo response 200; six manual themes, reduced motion and fallback tests passed |
| Independent audit | Astra high found one agenda-ordering P2; fixed, reproduced with a failing-before/passing-after rendered regression and independently rechecked. No remaining actionable findings identified |
| Restart with seeding enabled | All 20 application table counts and deterministic content digests unchanged; schema validation/startup passed |
| Hosted CI at final HEAD | Final published check results and commit are recorded on PR #52; local results do not substitute for them |
| Cross-team review, merge and shared-database rollout | Human/operator delivery gates; not inferred from local checks |

Calendar downloads use independent UTC events with stable occurrence UIDs. A client
may duplicate events or ignore edited/cancelled snapshots. Reducing the count does
not remove older imported occurrences; remove obsolete copies manually or import
into a fresh test calendar. There is no OAuth/two-way sync or client-side whole-series
editing promise. Relationship revocation blocks later exports, including cancellation
downloads, and cannot erase an offline calendar copy.

The broader team gates remain latest-Figma/contrast/projector/browser review, author
walkthrough, E1 implementation, materials/rehearsal and the actual submission receipt.

## Narrow baseline integration correction

The E2 mutual-availability check exposed a timezone-sensitive JDBC round-trip defect:
a stored weekly `10:00` could return as `17:30` under the Singapore JVM default.
Personal `AvailabilitySlot` and group `GroupAvailabilitySlot` start/end fields now
use direct `SqlTypes.LOCAL_TIME` mappings. These four annotations are a narrow shared
Team B entity correction; no matching formula, timezone meaning or SQL schema is
changed. Session civil date-time fields also use direct local mappings. The completion
evidence must verify API write/read against SQL under Singapore and UTC before
claiming the regression resolved in a deployed environment.

## Completion observations

Two isolated Chromium contexts used synthetic students against the migrated Java application.
The 23 saved browser checks covered the shared timer and automatic break, co-host controls and
forbidden role changes, reconnect/leave, native layered audio consent/local controls, 503
heartbeat recovery, weather/themes, group/buddy forms and cancellations, real Home/agenda/
notification integration, shared availability, draft protection and calendar privacy. Actual
membership removal/replacement/readmission rejected the old heartbeat and over-capacity join.
Desktop and narrow-layout screenshots were inspected. Provider failures are separately covered
by the deterministic weather cache/fallback tests; no failure of the actual provider is claimed.

A populated repeat now retains current layered audio, sessions and both cancellation/session
notifications. Historical notification enum subsets are temporarily installed without scanning
newer rows inside the guarded all-migration transaction; the final complete enum is validated
before commit. The regression checks retained data and final constraint validation.

The personal/group civil-time round-trip regression passed under both Singapore and UTC JVM
defaults. This is local verification; the schema/time mapping correction is not a deployment claim.

The existing owner-profile hook lint warning is retained. The completed production bundle
is approximately 555 KB before gzip and triggers Vite's nonblocking size advisory. No dependency, extra CI job, browser-test framework or external
music service was added.

The final audit corrected the group agenda preview to sort by each series' next occurrence
before selecting three; an imminent one-off can no longer disappear behind older weekly
series. Calendar evidence used isolated synthetic test calendars, without invitations or
attendees. Google and Apple test data were removed after the owner approved cleanup; no
Outlook test calendar was created. File imports are snapshots and client-specific observed
cancellation behavior is not a general synchronization guarantee.
