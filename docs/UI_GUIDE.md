# UI guide

The visual direction, the shared React components to reuse and what is still to check. The
rules every screen must follow are in [AGENTS.md](../AGENTS.md#frontend-conventions); screen
behaviour decisions are in [design decisions](DESIGN_DECISIONS.md#interface-behaviour).

## Design source of truth

**Figma:** https://www.figma.com/design/OPbwyiH0iyinAnJl5zKalm/OOPs

Figma wins over this guide wherever the two disagree. If a screen is not in Figma yet, ask
before inventing one.

## Visual direction

Direction from nat's inspiration board: **pixelated, retro, desktop operating system**. A
late-nineties / early-noughties desktop metaphor: window chrome with a title bar and controls,
pixel icons, chunky bevelled buttons, and a pixel-art loading screen with a percentage bar.
Reference sites: studiodunbar.xyz, chusmargallo.space (stacked movable windows), mitchivin.com
(Windows XP-style login), peteroravec.com.

The wireframes take the metaphor literally: each page is a window with a title bar (Home,
Student profile, Matching, Notifications, Study prep, study timer).

| Choice | Implementation |
| --- | --- |
| Window chrome | Decorative title bars with conventional routes and navigation; there is no movable stacking-window manager |
| Shared look | Authenticated screens and the landing page use the retro components in `frontend/src/shared/components/`; Matching adds its own score bars and a heatmap |
| Fonts | Tahoma, Verdana and system fonts; no web font and no pixel font is loaded |
| Styles | Palette and shared styles live in `frontend/src/shared/desktop.css`. `frontend/src/index.css` holds only a CSS reset adapted from Tailwind's preflight (MIT, credited in the README) |
| Narrow layouts | Navigation, forms, weekly schedules and admin rows wrap; nothing is declared desktop-only |
| Icons and art | Original `PixelIcon` CSS drawings. Streamline's pixel range was an inspiration, not a shipped dependency |
| Third-party assets | Listed in the [README](../README.md#libraries); verify the licence of anything new before distribution |

## Shared components

Whoever needs a primitive first builds it, and everyone else reuses it. Before building a
shared element, check this table and say that you are adding it.

| Need | Component (`frontend/src/shared/components/` unless a path is given) |
| --- | --- |
| App shell and navigation | `AppShell`: menu bar (notifications, help, account) and a sidebar grouped into Connect, Study and You with `NavSection` |
| Tabs within a page | `TabBar` |
| Window frame / page container | `RetroWindow` (title bar and content, also used by the landing page), wrapped by `WindowPage` for app screens |
| Menu bar | `MenuBar`: skip link, brand and the screen's own tools; used by `AppShell` and the landing page |
| Button (primary, secondary, destructive) | `Button` |
| Form field, label, validation message | `Field`, `AccountFields`, `FormActions` |
| Modal / dialog | `Dialog`, `ConfirmDialog` |
| Empty, loading and error states | `StatePanel` |
| Weekly availability grid | `WeeklySlotEditor`, `WeeklySchedule` |
| Toast / notification | `ActionNotice`, `RouteNotice`; unread badge in `AppShell` |
| Score breakdown bar | `features/matching/ScoreBreakdown` |
| Shared-hours heatmap | `features/matching/SharedHoursHeatmap` |

## Still to check

Narrow-layout and keyboard checks are listed in the [testing guide](TESTING.md). Comparing
screens with the latest Figma, contrast on every screen and readability on the projector are
still for the team to check.
