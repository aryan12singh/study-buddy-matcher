# E2 scope and delivery record

PR [#52](https://github.com/aryan12singh/study-buddy-matcher/pull/52) merged the complete
selected [E2 #16](https://github.com/aryan12singh/study-buddy-matcher/issues/16) as
`66d121a`. The original room-only plan was expanded, with owner authorization, to
sessions/calendar and the weather enhancement described below. The E2 schema was
applied to shared Supabase on 16 October; the [rollout record](DATABASE_OPERATIONS.md#e2-rollout--16-october-2026)
contains the checks and limits. This file now records delivered scope and boundaries;
it is not an active implementation checklist.

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

The owner approved a contained usability pass on the same PR: accurate ongoing-session
labels, direct safe room actions on current/upcoming cards, an accessible phase-transition
cue, remembered browser-local audio volume/mute and duration-aware mutual suggestions.
These use existing authorized DTOs and native browser capabilities; they introduce no API,
schema, library or provider changes. The existing disclosure file is unchanged in this pass.

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
separate AI match-explanation feature. The later [Achievements extension](ACHIEVEMENTS_TESTING.md)
is also separate from E2; it derives current standings from existing records and adds no
permanent awards or completed-focus tracking. Public rooms, chat, task boards,
shared notes, global online status, group-score recommendations and AI
roadmap/helper/icebreaker proposals remain unselected. Completing E2 does not
mean every discussion idea has been adopted or that the whole coursework submission
is complete.

## Design and integration

Room and session controllers validate input and delegate to services. Assemblers
return DTOs, with contacts/emails/tokens excluded. Transition writes take account
locks before group/connection locks so admission and state changes serialize with
lifecycle operations. Room reads and existing-lease heartbeats recheck membership
without a group-row lock; renewal never creates an expired or deleted lease. Room
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

Shared integration files included `application.yml`, routes, Home, connection/group
entry points and notification enums/links. E2 retains the matching formulas, ranking,
privacy reveal and own-profile semantics. The full contract is in [API_CONTRACT.md](API_CONTRACT.md), choices in
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

The [database record](DATABASE_OPERATIONS.md#e2-rollout--16-october-2026) documents the
shared room/audio/session migrations and temporary schema/wiring check after merge.
It does not claim a persistent hosted application. Outlook import remains
owner-deferred because of school MFA. Final team/device/projector acceptance,
rehearsal and submission retain their own evidence; E1 remains separate PR #61 work.
