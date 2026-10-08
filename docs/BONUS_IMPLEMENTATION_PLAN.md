# Bonus foundation and teammate handoff

9 October 2026. Branch `feat/team-c/collaborative-study-room` starts from merged
`main` at `fadff38097870ce88c954cd5d4c4690630ef78ae`. Existing #48/#49 merge
history is preserved.

## Agreed scope

This branch delivers a working room foundation and leaves substantial bonus work
for teammates. It does not complete all of [E2 #16](https://github.com/aryan12singh/study-buddy-matcher/issues/16)
or implement [E1 #15](https://github.com/aryan12singh/study-buddy-matcher/issues/15).

| This branch | Teammate work after integration |
| --- | --- |
| Private room for accepted members of an open group | Dated group and buddy study sessions |
| Start/pause/resume/reset shared Pomodoro | Session agenda/editor and calendar export |
| Expiring presence and capacity checks | Mutual availability using Team A's interval contract |
| Leader-assigned host/co-host and shared audio selection | Session notifications and Team B dashboard integration |
| Basic retro screen, original native browser audio and local consent/volume | E1 explanations of Team A's actual score breakdown |
| Migrations, focused tests and documented contracts | Cross-team review, author walkthrough and bonus acceptance |

Aryan approved existing Team C retro components for the room screen because the
accessible Figma file has an inspiration board and no dedicated bonus frames.
Original built-in music/ambient presets were also approved. No external music
account, asset, provider, library or dependency is introduced.

## Sources and boundaries

[E2 #16](https://github.com/aryan12singh/study-buddy-matcher/issues/16),
[E1 #15](https://github.com/aryan12singh/study-buddy-matcher/issues/15) and
[decisions #41](https://github.com/aryan12singh/study-buddy-matcher/issues/41)
are the current bonus scope. The local `oop.pdf` discussion supplies the original
ideas; `IS442-StudyBuddyMatcherSystem.pdf` supplies the brief/rubric. The issues
record E1/E2 as the selected extras for this eight-member team. Closed duplicate
proposals #35/#36 are not evidence of implementation.

Weather/seasons, badges, public rooms, chat, shared notes/task boards and AI
roadmap/helpers remain unselected proposals. Do not add them to this branch.

The baseline contains Team C plus the platform foundation. Open #50 changes
group/notification UX; open #51 changes profile/preferences/availability. Preserve
their work when integrating. Room changes to `routes.tsx`, the group entry link,
`application.yml` and migration assertions are additive shared integration points.
Matching scores/configuration remain Team A's work; full own-profile/dashboard
flows remain Team B's work.

## How this foundation works

`StudyRoomController` validates inputs and calls `StudyRoomService`. The service
checks active membership and locks accounts before the group, using the existing
advisory-lock protocol. `RoomAssembler` returns DTOs with names and room roles,
never contacts or entities. Room tables retain backend-only access.

`PomodoroTimer` is immutable and uses elapsed server time. `StudyRoom` persists an
anchor, remaining duration and version, rather than storing ticks. All shared
controls require the observed version. The timer runs through focus/break phases
until paused/reset, including during disconnects or server restarts.

Each room page has a UUID lease, scoped to its authenticated student. Multiple
tabs count as one student; leaving one tab removes only its lease. Heartbeats
expire after 30 seconds by default. Reads and controls recheck active membership;
expired presence must explicitly rejoin. The leader retains controls and a present
host/co-host can control the timer/audio. Browser polling is two seconds by default,
with local countdown rendering between snapshots. This is suitable for the small
coursework dataset; it is not a low-latency streaming or sample-synchronized player.

Audio is original synthesis in `RoomAudioEngine`, through native Web Audio. No
sound starts without local consent. Shared preset/play state is persisted; mute
and volume are local. Leave, access failure or navigation stops browser audio.

The full endpoint/DTO contract is in [API_CONTRACT.md](API_CONTRACT.md); choices
and lifecycle behaviour are in [DESIGN_DECISIONS.md](DESIGN_DECISIONS.md).

## Suggested next slices

1. **Sessions/calendar:** add a separate `studysession` package and feature folder.
   Keep weekly group availability unchanged. Agree organizer permissions, dated
   timezone-aware start/end, update/cancel behaviour and private buddy access.
   Record endpoints before building clients. Calendar exports need stable UIDs,
   escaping, line folding, bounded recurrence and an honest update/import policy.
   Integrate event notifications and dashboard data with their owners. Verify real
   imports before claiming Google/Apple/Outlook compatibility.
2. **Availability suggestions:** consume an agreed Team A overlap contract. Do not
   duplicate scoring/interval maths or treat suggested availability as attendance.
3. **E1 on its own branch:** wait for real matching results and agree a provider.
   Explain existing scores without changing ranking. Allowlist outbound signals;
   exclude names, contacts, emails, IDs, messages, credentials and full schedules.
   Add backend secret/timeout/limits configuration, evidence-aware cache invalidation
   and deterministic fallback. Provider failure must not break matching. Record
   genuine examples before calling E1 complete.

## Verification and delivery

Run `scripts/test-backend.sh` for disposable PostgreSQL migration/HTTP/concurrency
checks, then frontend `npm test`, `npm run lint` and `npm run build`. The new focused
suites are `PomodoroTimerTest`, `StudyRoomHttpTest`, `timer.test.ts` and
`StudyRoomPage.test.tsx`. Database boundary checks include both new room tables.

Local results and browser observations are recorded in
[REQUIREMENT_COVERAGE.md](REQUIREMENT_COVERAGE.md). The new migration is not applied
to shared Supabase by this branch. Apply it through the documented reviewed
migration workflow before running the feature there. Keep #16 open until the
remaining session/calendar work and final acceptance are complete.
