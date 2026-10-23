# Help and getting started verification

## Scope

The student-only `/help` page guides profile setup, deterministic matching, buddy requests,
groups, room previews, dated study sessions and calendar snapshots. It uses the shared retro
components and original CSS drawings, with no new dependencies, fonts, tracking API or schema.

Help reads the existing authorized endpoints once per resource, with the normal background
refresh behavior:

- `GET /profile/me`
- `GET /students/me/summary`
- `GET /groups/mine`
- `GET /sessions/mine`

The shared shell separately reads the unread notification count. Opening Help does not
request an AI explanation, join a room, start sound or create a session. The Home Help entry
reuses its already-loaded setup state and adds no request.

## Current state and recommendation contract

Five checks can be confirmed from current records: courses/study mode, weekly availability,
a pending outgoing request or active connection, membership of an open group, and an ongoing
or upcoming dated session. A completed, cancelled or past session does not satisfy the last
check. Weekly occurrences advance through the same helper and view clock as session lists.

`Saved in your profile`, current relationship/group/session labels, `Checking current records`,
`Could not check` and `Not checked` distinguish known, missing and unavailable information.
Searches and room visits are explicitly untracked. The guide does not infer a search from a
sent request or claim to know calendar downloads or attendance. Current checks are not a
lifetime achievement history: ended connections, closed groups and past dates can stop
satisfying them.

The one next action follows this order:

1. Finish required courses/study-mode or weekly-availability setup, using Home's existing helper.
2. Review confirmed incoming requests.
3. Review the next ongoing/upcoming authorized session.
4. Open a led group's dated agenda, or preview an accepted open group's room.
5. Choose a connected buddy to plan a session.
6. Browse groups while an outgoing request is pending.
7. Search for buddies when the required profile is ready and no higher action applies.

A source needed to choose that action must be known. A failure keeps the guide links usable
and offers a retry of only failed sources. A higher-priority confirmed action, such as profile
setup or an incoming reply, does not wait on unrelated groups or sessions.

Only active, accepted group membership supplies a direct `/groups/:id/room` preview or
`/groups/:id/sessions` agenda link. Opening a preview does not join the room. Unknown, denied,
pending or closed-group states use the ordinary groups page. Per-account resource keys and
purging prevent an older account or revoked membership from leaving its shortcut visible.

## Automated checks

From `frontend/`:

```sh
npm ci
npm test -- --maxWorkers=1
npm run lint
npm run build
```

The 23 October 2026 local run passed **361 tests in 40 files**, lint and the production build.
Help's two focused files cover recommendation priorities, initial unknowns, failed sources and
retry, current versus untracked checks, cancelled/expired/weekly dates, active membership,
account changes and late responses, permission invalidation, merged routes, full-shell topic
focus and searchable FAQ disclosures. Existing Home, matching, group, connection and session
tests assert their contextual links. Route tests redirect administrators and signed-out users
without fetching the four Help resources.

## Rendered browser checks

A local Chromium pass on 23 October used synthetic API fixtures, including a new student,
completed profile, incoming request, accepted member, group leader, connected buddy, future
session and denied source. Its **22 checks passed** with zero page errors or unexpected API
paths. These are rendered frontend checks; they do not establish real-backend or AI-provider
acceptance.

The pass checked desktop at 1440px and mobile at 390px and 320px, with no horizontal overflow.
It followed Home, matching, groups, connections and sessions Help links to focused topic
headings. Keyboard checks covered topic jumps, Tab into FAQ search, Enter/Space disclosure,
no-result reset and return of focus to the search field. The admin check used a fresh page
and attributed network requests to that page. Screenshots were reviewed for wrapping, action
visibility and legibility. Opening Help made no room join or provider call.

## Repeatable integrated walkthrough

Use the existing local demo or disposable integration setup, with synthetic accounts:

1. Open Help with missing courses/study mode and availability. Confirm one setup action and
   separate per-step labels. Save each profile section and return to Help.
2. Send a request or accept one. Confirm only the current relationship check changes; search
   history stays untracked. A pending incoming reply takes priority.
3. Apply to an open group. A pending applicant gets no room shortcut. After acceptance, the
   member can open a preview and must explicitly select **Join study room**. Verify leader
   and member recommendations separately.
4. As leader, open the dated agenda and plan a session; as connected buddies, plan from the
   connection card. Confirm the actual session is the recommendation. Review a downloaded
   `.ics` snapshot, then edit/cancel and confirm the app explains re-export rather than sync.
5. Remove membership, close the group or disconnect. Refresh or invalidate resources and
   confirm private shortcuts disappear. Repeat with a failed request and a different account;
   unavailable records must never be marked incomplete or reused from the earlier account.
6. Open each contextual Help link and use only the keyboard through topic navigation and FAQ
   controls at desktop and narrow widths. Confirm error retry leaves the other links usable.

Real-backend integration after branch combination is recorded separately. Latest-Figma
comparison, other-browser/contrast checks, physical-projector readability, cross-team review
and author explanation remain human acceptance gates. Local checks do not claim those sign-offs.
