# Team C UI polish

Approved and implemented on **7 October 2026** on the existing
`feat/team-c/match-request-state-machine` branch and
[PR #48](https://github.com/aryan12singh/study-buddy-matcher/pull/48).
All eight proposed polish items and the small retro delighters are complete in source and
locally verified. E1 AI and E2 collaborative rooms/timer/audio/dated sessions remain their
existing separate feature scope and issues. Human cross-team review, authors' walkthrough,
latest-commit hosted checks and squash merge remain delivery gates.

## Completed behaviour

| Item | Implemented result | Verification |
| --- | --- | --- |
| Visual hierarchy | Consistent shared spacing, clearer headings/sections, original pixel navigation glyphs, primary actions and deterministic initials | Rendered screens and current desktop screenshots; keyboard focus visible |
| Mobile polish | Compact shell identity/navigation; semantic desktop account table becomes labelled cards below 800px, with every field/action retained | Actual 375px admin and group form checks; no document-width overflow; actions at least 44px |
| Destination feedback | Group/account create/update and account deletion announce success on the destination, with dismissal and history-state consumption | FormPolish create/update/delete tests; actual saves/admin lifecycle; reload does not replay the message |
| Browsing | URL group/notification tabs and validated group filters, result counts, removable filter chips, clear actions and useful empty CTAs | GroupsBrowsing and NotificationsPage tests; actual filter/back/reload/clear with backend reads |
| Group cards | Course/mode/leader hierarchy, accepted-member capacity meter and current leader/member/pending/full/closed states | Existing HTTP lifecycle/concurrency suite, GroupsBrowsing states, actual approval changed meter to 2/4 |
| Schedules | Independently duplicate blocks; copy a snapshot to chosen other days without adding identical blocks; sorted live weekly preview | WeeklySlotEditor tests and native UI; saved GET contains Tuesday/Thursday/Friday 09:00–11:00 (SGT) |
| Convenience | Canonical group-link copy and already-authorised contact copy; native clipboard failure gives useful manual guidance | CopyButton/privacy tests; actual clipboard success, forced denial and disconnect purge |
| Form protection | Sticky save controls/status; dirty Cancel/sidebar/Back/reload guards; buddy/application Cancel/X/Escape discard choices; clean/saved forms leave normally | FormPolish, navigation and DialogDrafts tests; actual Back/sidebar and dialog keep/discard |
| Retro delighters | CSS pixel empty artwork/loading skeletons, indeterminate loading bar, initials avatars and subtle button transitions | Decorative content is aria-hidden; actual reduced-motion transition is 0s; no fabricated progress/presence |

## Shared integration boundary

The existing route paths, role guards, auth provider and API client are reused. `routes.tsx`
now exports the route tree; `main.tsx` creates **one** `createBrowserRouter` outside StrictMode,
and `App.tsx` wraps its `RouterProvider` in the existing `AuthProvider`. `AccountHome.tsx`
contains the unchanged role-based account redirect. This is the existing React Router's
supported data-router foundation for `useBlocker`, not a custom history interception layer.
Rendered form tests use an equivalent memory data router. A/B add routes to the same tree;
do not mount another router/provider/client around C forms or matching request dialogs.

Shared existing components changed: `AppShell`, `StatePanel`, `ActionNotice`, `Dialog`,
`WeeklySchedule`, `WeeklySlotEditor`, `useViewNavigation` and `shared/desktop.css`.
New reusable primitives are `PixelIcon`, `Avatar`, `CapacityMeter`, `CopyButton`,
`FilterSummary`, `RouteNotice`, `FormActions`, `useFormExit` and `useDraftClose`.
`useViewLifetime` is exported beside the navigation guard for abandoned-dialog callbacks.
The pixel patterns are original CSS cells with system-font fallbacks; no font/icon package
or externally sourced illustration was added. Other teams can reuse these primitives.

C changes cover groups browse/card/detail/form/management, connections/request dialog,
student profile, notifications, admin list/detail/form and their rendered tests.
Matching algorithms, full own-profile/preferences/dashboard screens and the landing-page
asset-provenance question remain with their existing owners.

## Backend and state decisions

No backend source, API DTO, endpoint, migration, schema, configuration or dependency change
was needed for this pass. The **actual running backend** supported group saves, membership
approval, contextual buddy requests, accept/disconnect privacy and all account lifecycle
operations. The complete existing backend suite was rerun against fresh isolated PostgreSQL.
UI permission/capacity indicators use server DTO fields and do not replace backend decisions.

Group tabs use `view=browse|mine|applications` (browse is the default); valid `courseId`,
`studyGoal` and `studyMode` are the existing browse filters. Notification tabs use
`view=requests|groups` (default ALL). Tabs create history entries; group filter edits replace
the current entry. Invalid enum/numeric inputs fall back safely; unrelated URL keys are
preserved but never sent to the API. Result text is scoped to the current view, not a fabricated
global/unread count.

Drafts remain only in mounted memory. Reload warnings are registered only while dirty or
pending and removed on unmount/session expiry. No draft, contact, password or token enters
query strings or destination success state. Generic success messages are consumed from
history so Back/reload does not repeat them. Leaving a pending write cannot undo the submitted
backend transaction; completed writes still refresh resources, but abandoned views cannot
redirect or invoke their old dialog completion callbacks.

Group links use the current client origin plus `/groups/{id}`, excluding query/hash.
Contact copy appears only when an authorised self/CONNECTED payload supplies a nonempty
contact. Denied clipboard access shows manual guidance; a contact is not duplicated in the
fallback. Existing disconnect/logout/401 purging removes the copy control along with private
data. Nested dialogs retain body scroll lock until the final modal closes, including
out-of-order unmount on navigation.

## Actual verification

| Check | Result |
| --- | --- |
| Fresh Java 21 / PostgreSQL 17 backend runner | **228 passed across 27 suites**, 0 failures/errors/skips; migration upgrade/repeat/preflight checks passed |
| Fresh locked frontend install, official Node 22.23.3 container | **108 passed across 16 suites**, 0 lint warnings/errors, TypeScript/production build passed; npm audit **0 vulnerabilities** |
| Local Node 26 interaction/lint/build run | Same 108 tests passed; lint/build passed |
| Actual Vite app + Java backend + PostgreSQL | Sequential authenticated Priya/Jamie/Alex/admin flows; synthetic fixtures only, no shared Supabase writes |
| Browser console after fresh navigation | 0 errors and 0 warnings in the final journeys |

The polish adds **31 rendered regression tests** to the previous 77-test frontend baseline.
They cover navigation/discard/history, native reload listener cleanup, successful
create/edit/delete destinations, clean/reverted forms, nested-dialog scroll restoration,
session expiry, stale callbacks, valid/invalid URL filters, capacity/viewer flags, weekly
copy/preview, clipboard denial/value changes and immediate contact-copy purge. Existing
privacy/draft-refresh tests now target their loading view explicitly when a dialog also
announces its form save state.

Live browser checks used a disposable PostgreSQL 17 container, private generated runtime
credentials and a backend profile that does not load the shared Supabase connection. The
normal existing 50-student/10-course opt-in seed supplied synthetic accounts. Browser actions
created/edited a group, copied its link, saved three recurring weekly slots, applied/approved
membership, sent/accepted/disconnected a buddy request and copied its authorised contact.
A disposable admin-created account was edited, deactivated, reactivated and **permanently
deleted through the UI**, returning to the account list with the success notice. The seed
account count returned to 51. No screenshot contains a credential or real private contact.

Both buddy and application draft Cancel/X/Escape paths were exercised without submitting
the drafts. Dirty group Back/sidebar prompts preserved edits on Keep editing and discarded
only on explicit confirmation. Native clipboard denial exposed the canonical-link fallback.
Narrow admin cards retained their actions; native computed reduced motion removed transitions,
and the narrow group form displayed the sticky action bar and saved weekly preview.
These are the observed browser/viewport checks, not a claim of every browser, assistive
technology or the team's projector rehearsal.

Implementation commits: `fa7dcc5` (shared retro feedback, controls and data router) and
`27ddb45` (C browsing/account/request workflows). The documentation commit records these
verified source changes; hosted results for the pushed revision are linked from PR #48.

## Current screenshots

- [Desktop filtered group list](images/team-c-groups-polished.jpg)
- [Saved group with accepted members and recurring schedule](images/team-c-group.jpg)
- [Narrow account cards and filters](images/team-c-admin-narrow.jpg)
- [Buddy request draft with protected form actions](images/team-c-request-draft.jpg)
- [Narrow weekly editor and live preview](images/team-c-schedule-preview.jpg)

The screenshots show actual saved/edited synthetic data from this isolated running app.
Reproduce the supported suites with the commands in [TEAM_C_TESTING.md](TEAM_C_TESTING.md).
The PR checks link is the source of truth for the latest pushed SHA. Cross-team review,
both owners' walkthrough, A/B product integration, team materials/rehearsal and merge
remain separate responsibilities; there is no unfinished polish item in this approved scope.
