# Study room reactions and the focus forest

Feasibility check and build plan, written 29 Oct 2026. Branch:
`feat/team-c/room-reactions-and-forest`.

**Status (29 Oct): built.** Decided: one tree per continuous room visit; the solo timer does not
grow trees. Changes from the proposal below: variety comes from the visit's local start time
(dawn, day, dusk, night colours) rather than the room atmosphere, because the atmosphere is chosen
per browser and never reaches the server; the live tree polls its own endpoint instead of riding
on the room snapshot; the forest has its own page (`/forest`) linked from achievements. Not built
(stretch): the Forester badge and the group grove.

## 1. What we want

1. **Reactions in the study room**, like Zoom or Teams: press a reaction, everyone in the room
   sees it float up for a few seconds. Kept in our retro desktop / forest look: original pixel
   sprites drawn with the same `PixelRects` helper the achievements page uses, chunky bevelled
   reaction tray, no emoji font.
2. **A visual forest**, like the Forest app: **one tree per study session**, and a longer session
   grows a better tree. Shown with the same pixel style as the existing `StudyGrove`.

## 2. What exists today (and the limits that matter)

| Area | Today | Consequence |
| --- | --- | --- |
| Room timer | Shared Pomodoro per **group** room. Focus 1–180 min (`max-focus-minutes`), break 1–60 min, default 25/5. Runs focus → break → focus forever until paused or reset. | A single focus block can be up to 3 h; a room visit has no upper limit. |
| Timer engine | `PomodoroTimer` is projected from server time; there is **no scheduler** and completed phases are **not recorded anywhere**. | Nothing to count trees from yet. We need a new record of focused time. |
| Presence | Browser tab leases (`room_presences`), renewed by a heartbeat on every poll (2 s). Lease lasts 90 s. Heartbeat already says `PRESENT`, `FOCUS` or `BREAK`. Hidden tabs are throttled to ~1 poll a minute. | We can credit focus time on each heartbeat, capped at the lease lifetime so a throttled tab is fine but a dead one earns nothing. |
| Realtime | Plain HTTP polling every 2 s. No WebSockets, and the stack is fixed (no new libraries). | Reactions reach others within ~2 s. Fine for a demo; not instant like Zoom. |
| Solo timer | `PersonalPomodoro` is browser-only (`sessionStorage`), no server record. | Solo study cannot grow trees unless we add a server record for it (see open question 2). |
| Rooms need a group | `study_rooms.study_group_id` is `NOT NULL`. | A student with no group has no room, so no reactions and (without question 2) no trees. |
| Current grove | `StudyGrove` on the achievements page: one tree per **week** of the planned-session streak, three growth stages. | Different idea from Forest. Keep it as the streak view; the new forest sits beside it. |
| Migrations | Every SQL file replays on each start (see #82). | New SQL must be idempotent (`IF NOT EXISTS`); a new notification type means updating the check-constraint list in `20261024010000_study_room_started_notification.sql`. |

**Verdict: both features are feasible with the current stack and no new dependencies.** The
real risks are time and ownership, not technology:

- **Freeze.** `DELIGHTERS_PLAN.md` puts code freeze at **1 Nov**; `AGENTS.md` still says 24 Oct.
  Today is 29 Oct. The full scope below is about 2–3 focused days; the cut line in section 6
  keeps a demo-able version inside the freeze.
- **Ownership.** Study room code was written mostly by nat and jo; achievements by charlize.
  Reactions edit `StudyRoomService`, `StudyRoomDto` and `StudyRoomPage`, so announce the work
  and get one of them to review before touching those files.

## 3. Reactions

### Behaviour

- A reaction tray in the room (a small window-chrome bar under the timer, collapsible like the
  other panels). Six reactions, each an original 12×12 pixel sprite:
  **clap, heart, fire ("on a roll"), lightbulb ("got it"), coffee ("brb / break"), sprout
  ("growing!" — the forest tie-in)**.
- Pressing one sends it; every participant sees it rise from the bottom of the room scene with
  the sender's first name, then fade (about 4 s). `prefers-reduced-motion` shows a static chip
  instead of the float.
- Only students currently present in the room can send. A per-student cooldown (configurable,
  default 1 s, burst of 5 per 10 s) stops spam.
- Keyboard: tray buttons are real buttons with labels ("Send clap reaction").
- Not stored as history. Reactions older than the visible window are dropped.

### Design

- **Backend** (package `com.studybuddy.studyroom`):
  - `RoomReactionType` enum (the six types).
  - `RoomReaction` entity in a new table `room_reactions` (id, study_room_id, student_id, type,
    created_at), indexed on `(study_room_id, created_at)`.
  - `RoomReactionService.send(groupId, actorId, type)` checks a live lease via the existing
    `RoomAccess`, applies the throttle (`ReactionThrottle`, config-driven), saves, and deletes
    rows older than the retention window for that room (same lazy clean-up pattern as expired
    leases, no scheduler).
  - `POST /api/groups/{groupId}/room/reactions` with `{ type }` → 202 (controller only validates
    and calls the service).
  - `StudyRoomDto` gains `List<RoomReactionDto> recentReactions` (id, type, senderName,
    createdAt) for the last `reaction-window` seconds. It rides on the poll that already
    happens, so no new polling loop.
  - Config: `app.study-room.reactions.window`, `.cooldown`, `.burst-limit`, `.burst-window`
    (+ README configuration rows).
- **Frontend** (`features/studyroom/`):
  - `ReactionTray.tsx`, `ReactionLayer.tsx` (renders floats; remembers ids it has shown so a
    reaction is animated once), `reactionSprites.ts` (pixel rows + palette, reusing
    `PixelRects`), `reactions.css`.
  - Own reactions show immediately (optimistic), then de-duplicate by id on the next poll.
- **Tests:** throttle unit test, service test (not present → 409, unknown type → 400, old rows
  pruned), HTTP test for the endpoint, component test that a new id animates once.

## 4. The focus forest

### What counts as "a session" (recommended; see open question 1)

**One tree = one continuous visit to a study room** (join → leave, or the lease expiring).
During the visit, the server credits **focused minutes**: time when the room timer is in a
running focus phase and the student's tab reports `FOCUS`. Breaks and pauses do not count.
When the visit ends, the tree is planted with its final size. Visits under the minimum (default
10 focused minutes) plant nothing, so drive-by joins don't litter the forest.

This matches "one tree per session" and the Forest rule that you have to stay to grow. It is
kept gentle: leaving early still plants whatever stage you reached; there is no dead tree.

### Tree tiers (all thresholds in `application.yml`)

| Focused minutes | Tree | Look |
| --- | --- | --- |
| 10–24 | Sprout | two leaves |
| 25–49 | Sapling | the existing `SAPLING` sprite |
| 50–89 | Oak | the existing `GROWN` sprite |
| 90–149 | Tall pine | new, taller 12×24 sprite |
| 150+ | Ancient blossom | new, wide canopy with pink blossom pixels and a little sparkle animation |

Variety without randomness: the tier sets the shape; the room's **atmosphere** at planting time
(rain, snow, sunny, night — already chosen in `RoomWeatherPanel`) tints the leaves, so two oaks
can still look different. Deterministic, so a marker can ask "why is this one blue?" and get an
answer.

### Where it shows

1. **In the room: "Your tree"** — a small window in the room showing the tree growing live as
   focused minutes rise, with "12 min to the next stage". This is the Forest moment in the demo.
2. **Achievements page: "My forest"** — a pixel meadow of planted trees, newest first, grouped by
   week, with totals (trees, focused hours, best session). Hover/focus a tree for date, group,
   minutes. Empty state: "Your forest is a seed — join a study room and stay for a focus block".
   The existing planned-session grove stays as the streak view.
3. *(Stretch)* **Group grove in the room:** today's trees of everyone in the group.

### Design

- **Backend** (new package `com.studybuddy.forest`, owned by Team C next to achievements):
  - `FocusVisit` entity, table `focus_visits` (id, student_id, study_group_id, started_at,
    last_credited_at, ended_at nullable, focused_seconds, atmosphere, tier nullable).
  - `FocusCredit` pure function: given the room timer snapshot, previous credit time, now and the
    lease cap, returns the seconds to add. Heavy unit tests (pause, break boundary, phase that
    ended between heartbeats, throttled tab, clock going backwards).
  - `TreeTier` enum + `TreeTierPolicy` (config-bound thresholds → tier).
  - `ForestService`: `credit(...)` called from the room heartbeat; `closeVisit(...)` called when a
    lease is released or found expired; `forestOf(studentId)` for the page.
  - Hook-in to the room: `StudyRoomService` publishes small Spring events (`RoomPresenceRenewed`,
    `RoomPresenceEnded`) and `ForestService` listens — one `publishEvent` line per call site, no
    forest logic inside the room code (same pattern as achievements).
  - `GET /api/me/forest` → `ForestDto` (trees, totals). Room snapshot gains
    `currentTree { focusedMinutes, tier, minutesToNext }` for the live widget.
  - Optional: a "Forester" badge row in `achievements` (trees planted: bronze 1 / silver 5 /
    gold 15) using the existing badge machinery.
- **Frontend:** `features/forest/` with `ForestMeadow.tsx`, `TreeSprite.tsx`, `treeSprites.ts`
  (moves the grove's sprite rows into one shared place, reused by `StudyGrove`), and
  `features/studyroom/GrowingTree.tsx` for the live widget.
- **Seed:** add a few finished visits to `demo-data.json` so the demo forest isn't empty.

### Anti-cheat level (honest)

A student who leaves a tab open in focus mode grows a tree; that is also true of Forest. We only
guarantee that time is counted from **server** time, only while the shared timer is really in a
running focus phase, and never more than the lease lifetime per heartbeat.

## 5. Open questions (yours to answer)

1. **What is "a session" for a tree?** Recommended: one continuous room visit (above).
   Alternatives: one completed focus block (many trees per visit), or one planned
   `StudySession` attended (needs attendance tracking that doesn't exist).
2. **Should the solo timer grow trees?** Today it is browser-only. Adding it means a small
   server endpoint and trusting the browser more. Recommended: not in this PR.
3. **Is the forest owned by Team C** (beside achievements), and are nat/jo happy for reactions
   to land in the study room files?
4. **Freeze date:** is 1 Nov the real cut-off? It decides whether the stretch items happen.

## 6. Build order, branch and PR

All on `feat/team-c/room-reactions-and-forest`, one draft PR into `main`, small commits:

| # | Commit | Must-have before freeze? |
| --- | --- | --- |
| 1 | `docs: plan study room reactions and focus forest` (this file) | yes |
| 2 | `feat: add room reactions endpoint and throttle` (+ migration, config, tests, API contract) | yes |
| 3 | `feat: show reaction tray and floating pixel reactions in the study room` | yes |
| 4 | `feat: credit focused minutes during study room visits` (`FocusCredit`, `FocusVisit`, events, tests) | yes |
| 5 | `feat: plant a tiered tree when a room visit ends` + `GET /api/me/forest` | yes |
| 6 | `feat: show the forest meadow on the achievements page` | yes |
| 7 | `feat: grow a live tree in the study room` | yes (it is the demo moment) |
| 8 | `feat: seed a demo forest` | yes |
| 9 | atmosphere tint, Forester badge, group grove | stretch |
| 10 | `docs: record reactions and forest in API contract, README config, requirement coverage, AI usage` | yes |

PR description covers: what changed, ownership flag for study room files, shared files touched
(`application.yml`, `StudyRoomDto`, routing if any), how it was tested (unit + HTTP tests, two
browsers in one room for reactions, a 25-min and a 10-min visit for trees), and the checks (no
logic in controllers, no entities returned, unauthorised/empty cases).

## 7. Out of scope

WebSockets or any realtime library; custom/free-text emoji; reaction history; dead/withered
trees; buying trees with coins; trees for planned sessions not attended in a room.
