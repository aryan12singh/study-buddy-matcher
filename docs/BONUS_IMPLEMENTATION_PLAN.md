# E2 implementation and delivery

PR [#52](https://github.com/aryan12singh/study-buddy-matcher/pull/52), branch
`feat/team-c/collaborative-study-room`, now targets the complete selected
[E2 #16](https://github.com/aryan12singh/study-buddy-matcher/issues/16).
The earlier foundation deliberately left session/calendar work to teammates;
the project owner has since authorized completing it in this PR. Latest merged
baseline work is integrated without rewriting existing merge history.

## Agreed scope

| Component | Behavior |
| --- | --- |
| Private group room | Accepted active members only; join/leave, participant list, focus/break state, distinct-student capacity and expiring tab leases |
| Shared Pomodoro | Configurable durations, server-time start/pause/resume/reset and observed-version conflict checks; state survives refresh/reconnect |
| Leader/delegated control | Leader assigns host/co-host; present assigned hosts control timer/audio and configure durations/capacity without changing roles; leader retains recovery control |
| Music and ambience | Independent original synthesized music and ambient selections/play state; intentional local enable, separate volume/mute and silent failure fallback |
| Dated sessions | Group leader and the two connected buddies manage authorized dated series, timezone, title, optional agenda/location/room link and whole-series update/cancel |
| Mutual availability | Existing Team A interval intersections across current participants; empty schedules stay empty and suggestions do not confirm attendance |
| Calendar export | Single or bounded weekly series expanded to stable individual UTC events; correct timezone/DST resolution, private authenticated export and honest re-import limitations |
| Product integration | Actual sessions in Home and group/buddy agendas; recipient-scoped change notifications; cancellation and parent lifecycle revoke access |
| Weather enhancement | Approved original pixel themes, day/night and optional backend-cached Open-Meteo campus conditions, with selectable fallback and reduced motion |
| Delivery | Ordered migrations, focused regressions, API/config/design diagrams, real workflow evidence and cross-team review |

The owner approved existing Team C retro components for these screens and original
built-in audio. No external music account, audio provider, asset pack or new Java/npm
dependency is introduced. Open-Meteo was separately approved for optional weather;
its source/terms/attribution and configuration are recorded in the README and decisions.

## Sources and boundaries

The formal `IS442-StudyBuddyMatcherSystem.pdf` supplies the brief and bonus rubric;
the latest local `oop.pdf` discussion supplies the original proposals. Selected
scope is recorded by #16 and [decisions #41](https://github.com/aryan12singh/study-buddy-matcher/issues/41).
The weather enhancement was expressly adopted after the foundation plan.

[E1 #15](https://github.com/aryan12singh/study-buddy-matcher/issues/15) remains a
separate AI match-explanation feature. Public rooms, chat, badges, task boards,
shared notes, global online status, group-score recommendations and AI
roadmap/helper/icebreaker proposals remain unselected. Completing E2 does not
mean every discussion idea has been adopted or that the whole coursework submission
is complete.

## Design and integration

Room and session controllers validate input and delegate to services. Assemblers
return DTOs, with contacts/emails/tokens excluded. Membership/account locks precede
group/connection locks, so changes serialize with lifecycle operations. Room
leases are separate from the durable timer/audio state and from dated sessions.
Removal, closure and deactivation clear leases/roles transactionally; readmission
requires a new capacity-checked join. Session access always checks the current
parent relationship, including after a notification link or calendar click.

The timer stores an immutable server-time anchor and remaining duration instead of
ticks. HTTP polling reconciles snapshots while the browser renders the countdown
locally. Multiple tabs count once toward capacity; leaving removes only that tab's
lease. A host disconnect does not pause the timer. The leader/co-host can recover,
and an expired host must rejoin before controlling the room.

Music and ambient layers share preset/play state. Native Web Audio requires local
consent and stops on exit/access loss. Local volume/mute never enters API state;
playback positions are not synchronized. Optional weather uses fixed configured
campus coordinates, never the student's location or account data, with bounded
network/cache behavior and visible stale/unavailable feedback.

Sessions remain a separate `studysession` feature. Weekly availability stays
campus-local and is reused through Team A's existing interval contract. Dated
series store local start/end plus an IANA zone and generate bounded UTC
occurrences. Missing/ambiguous DST endpoints are rejected. Group leader authority
and current buddy-participant authority are checked independently of room host roles.

Calendar files are snapshots of independent events, not an OAuth integration or
live subscription. Stable occurrence UIDs help clients recognize re-exports, but
imports may duplicate/ignore edits or cancellations. Reducing a series does not
delete an old imported occurrence, and ending access never removes an offline copy.
Real client-import acceptance remains distinct from automated format checks.

Shared integration files include `application.yml`, routes, Home, connection/group
entry points and notification enums/links. Their changes are additive and must be
flagged in the PR. Scorers, ranking, privacy reveal and own-profile semantics are
retained. The full contract is in [API_CONTRACT.md](API_CONTRACT.md), choices in
[DESIGN_DECISIONS.md](DESIGN_DECISIONS.md), and diagrams under `docs/diagrams/`.

## Verification and delivery gates

Use the existing two-job pipeline. Run `scripts/test-backend.sh` and frontend
`npm test`, `npm run lint`, `npm run build`. Keep focused pure timer/schedule/calendar
tests, HTTP permission/lifecycle/concurrency checks and UI workflow regressions;
avoid a new test framework or a second suite that mirrors every implementation line.

[E2_TESTING.md](E2_TESTING.md) records the current results and acceptance status.
The earlier room-only counts are historical evidence, not the current completion
result. Verify two authenticated browsers, layered audio, membership revocation,
dated-session dashboard/agenda/notification updates, export times and real imports
into Google Calendar, Apple Calendar and Outlook. Record gaps honestly.

The new room/audio/session migrations are not claimed applied to shared Supabase.
Use the reviewed [database procedure](DATABASE_OPERATIONS.md) before running this
branch against a hosted target. Source completion, local verification, hosted CI,
human review, merge, deployment and presentation acceptance remain separate gates.
Do not close #16 using an unavailable-client acceptance claim or mark E1 complete.
