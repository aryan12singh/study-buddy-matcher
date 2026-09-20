# Conventions

How we work in this repository: architecture rules, coding conventions, design direction and
process. One file, so there is one place to look and nothing to drift out of step.

Read it before you write code. Claude Code, Copilot, Cursor and Gemini CLI all pick this file
up automatically, so pointing your assistant here costs nothing, and it is why eight people
with AI assistance can avoid producing eight different architectures.

---

## What this project is

Study Buddy Matcher, a coursework project for IS442 Object Oriented Programming at SMU.
Students register, list courses and weekly availability, get scored matches against other
students, send match requests, and form study groups. Administrators manage accounts and tune
the matching weights. Full description in [`README.md`](README.md).

**The mark rewards object-oriented design, not cleverness.** Modular layered code, single
responsibility, externalised configuration, conventional Java, meaningful names. Optimise for a
marker reading the code, not for line count.

## Stack (fixed, do not substitute)

Java 21, Spring Boot, Spring Data JPA, Spring Security with JWT, Supabase Postgres, React with
Vite, Maven, npm.

Do not introduce a library, tool or service that is not already in the libraries table in
[`README.md`](README.md#libraries). If you believe one is genuinely needed, say so and stop.
Do not add it and carry on. If a dependency is agreed, add its row to that table in the same
pull request.

## Layout

```
backend/    Spring Boot application (Maven)
frontend/   React application (Vite)
docs/       API contract, UML diagrams
```

## Open questions to settle before coding

- **How is preferred group size represented?** The brief contradicts itself: the functional
  requirements list it categorically ("one-to-one, small group, or either") while Appendix A
  scores it numerically, treating "2-3 students" against "2 students" as compatible. A
  categorical reading is a small compatibility matrix; a numeric one is interval overlap like
  `TimeSlot`. `GroupSizeScorer`, the profile form, the seeder and the filter panel all depend
  on the answer. **Team A to decide.**
- **Is admin account deletion a hard delete or deactivation**, and what happens to that
  student's connections and group memberships? **Team C to decide.**
- **What counts as "basic usage information"** on the admin screen: last login, or counts of
  matches, connections and groups? **Team C to decide.**
- **Is the window chrome real or decorative?** See [Visual direction](#visual-direction) below.

---

## The two rules

Checked on every pull request. Not negotiable.

1. **Controllers never touch repositories.** A controller validates its input, calls one service
   method, and returns a DTO. No business logic, no query building, no repository injection.
2. **Services never return entities to the API.** Entities stay behind the service boundary.
   Services return DTOs built by an assembler.

The full flow:

```
Controller -> DTO / Assembler -> Service -> Domain model -> Repository -> DB
```

Rule 2 is also the privacy mechanism. A student's contact number must be invisible until a
match request is accepted, and that is enforced by `ProfileViewAssembler` choosing between
`PublicProfileDto` and `ConnectedProfileDto`. Return an entity from a service and the contact
number goes out with it. Never hide a private field in the React layer and call it done. The
value is still in the payload and visible in the network tab.

---

## Java conventions

- Standard Java naming. `PascalCase` types, `camelCase` methods and fields, `UPPER_SNAKE_CASE`
  constants. Packages all lower case.
- Names say what the thing is. `AvailabilityOverlapScorer`, not `Scorer2` or `Helper`. No
  `Util`, `Manager` or `Processor` classes that do six unrelated things.
- One class, one responsibility. If a class needs "and" to describe it, split it.
- Prefer an interface with implementations over a `switch` on a type. The matching engine uses
  `CriterionScorer` and `MatchingStrategy` for exactly this reason.
- Value objects (`TimeSlot`, `MatchScore`) are immutable. No setters.
- No magic numbers. Matching weights and thresholds come from configuration, never from a
  constant buried in a scorer.
- Constructor injection only. No field `@Autowired`.

## Configuration and secrets

- Anything tunable lives in `application.yml`, bound to a typed config object.
- Secrets come from the environment. Templates are per app:
  [`backend/.env.example`](backend/.env.example) and
  [`frontend/.env.example`](frontend/.env.example). Spring Boot reads the process environment,
  not `.env`; Vite reads `frontend/.env` directly. Anything `VITE_`-prefixed ends up in the
  built bundle and is public, so never put a secret there.
- **Never write a real credential into a file in this repository**: not into `application.yml`,
  not into a test, not into a comment, not as a "temporary" default. `.env` is git-ignored;
  keep it that way. If you think you have committed a secret, say so immediately. Rotating it
  is easy, but only if we know.
- Add a configuration key -> add a row to the configuration table in `README.md`, same change.

---

## Frontend conventions

- Function components with hooks. No class components.
- API calls go through a shared client that reads `VITE_API_BASE_URL`. Never hard-code
  `localhost:8080` in a component.
- Components in `PascalCase`, one per file, file named after the component. Shared components
  live in the shared directory; screen-specific ones live beside their screen.
- TODO: Team B to confirm the frontend directory layout once the Vite skeleton lands.

### Design source of truth

**Figma:** https://www.figma.com/design/OPbwyiH0iyinAnJl5zKalm/OOPs

Figma wins over this file wherever the two disagree. If a screen you need is not in Figma yet,
ask before inventing one.

### Visual direction

Direction from nat's inspiration board: **pixelated, retro, desktop-operating-system**. A
late-nineties / early-noughties desktop metaphor: window chrome with a title bar and controls,
pixel icons, chunky bevelled buttons, a pixel-art loading screen with a percentage bar.
Reference sites: studiodunbar.xyz, chusmargallo.space (stacked movable windows), mitchivin.com
(Windows XP-style login), peteroravec.com. Icon set under consideration: Streamline's pixel
range.

Current wireframes take the metaphor literally: each page is a window with a title bar: Home,
Student profile, Matching, Notifications, Study prep, study timer.

**Open decisions, settle these before building screens:**

- TODO: Do windows stack and move, or is the chrome decorative with conventional navigation?
  **Biggest decision here.** A real stacking window manager is a lot of frontend work that
  earns nothing on a mark scheme rewarding OO design. Decorative chrome gets the look for a
  fraction of the cost. Agree before anyone starts.
- TODO: Confirm the aesthetic with Team A and Team C too. The matching page carries dense
  information (score breakdown bars, a shared-hours heatmap) and must stay readable inside
  the chosen chrome.
- TODO: Pick the pixel font and check it is legible at body size on a projector. A marker
  reads this screen from across a room. If the font fights that, use pixel for headings and
  window titles only.
- TODO: Fix the palette and check contrast. Retro palettes are often low contrast.
- TODO: Decide the responsive story, or state plainly that the app is desktop-only.
- TODO: Confirm the icon set and its licence; add it to the libraries table if it ships.

### Shared components

Whoever needs a primitive first builds it, and everyone else reuses it. A one-off second
button is how a design system dies. Before building a shared element, check whether it already
exists and say that you are adding it.

The set we expect to need:

| Component | Status |
| --------- | ------ |
| App shell and navigation | TODO |
| Window frame / page container | TODO |
| Button (primary, secondary, destructive) | TODO |
| Form field, label, validation message | TODO |
| Modal / dialog | TODO |
| Empty, loading and error states | TODO |
| Weekly availability grid | TODO |
| Toast / notification | TODO |
| Score breakdown bar | TODO |
| Shared-hours heatmap | TODO |

### Rules for every screen

- **Every list has an empty state.** No matches, no requests, no groups, no notifications. An
  empty table with no explanation looks like a bug in a demo.
- **Every async view has a loading state and an error state.** The demo runs on SMU wifi.
- **Nothing sensitive is hidden with CSS.** A contact number is withheld by the backend or not
  at all.
- **Buttons say what they do.** "Send match request", not "Submit".
- **Destructive actions confirm first.** Leaving a group, removing a member, deleting an account.
- **Forms validate before submitting** and say what is wrong next to the field that is wrong.

---

## Team ownership (stay in your lane)

| Team | Members | Owns |
| ---- | ------- | ---- |
| Team A | nat, cy, jo | Matching engine: scorers, `TimeSlot` maths, strategies, `MatchScore`, matching config, matching and admin-config UI |
| Team B | angel, averyl, gh | Spring Boot and React skeletons, entities, enums, repositories, Spring Security and JWT, `ProfileService`, data seeder, shared UI shell, auth and profile screens |
| Team C | aryan, charlize | `MatchRequest` and its state machine, `Connection` (including ending one), `ProfileViewAssembler` and the profile DTOs, `StudyGroup`, `GroupMembership` and group join requests, admin users, notifications, and their screens |

Do not refactor another team's code as a side effect of your task. If their code blocks you,
raise it rather than rewriting it. A surprise refactor in someone else's file is how merge
conflicts and "I cannot explain that line" happen. Shared files (`application.yml`, `pom.xml`,
`package.json`, the routing table, the shared API client) are edited with care and flagged in
the pull request description.

---

## Process

### Milestones

| Milestone | Date | Means |
| --------- | ---- | ----- |
| Base code | 11 Oct | Every compulsory feature works end to end |
| Feature freeze | 24 Oct | Extras done, tests and polish landed, nothing new starts |
| Submission | 15 Nov, 11:59 PM | All deliverables in |

### Picking up work

1. Say what you are starting, wherever the team is coordinating. Two people building the same
   screen is the expensive mistake here.
2. Be clear what "done" means before you start: which behaviours, which edge cases.
3. If the requirement is ambiguous, ask. Do not guess at a graded requirement.

### Branches

Never commit to `main`. Branch from an up-to-date `main`:

```bash
git checkout main && git pull
git checkout -b feat/team-c/match-request-state-machine
```

Format `type/team/short-description`, where type is `feat`, `fix`, `test`, `docs` or `chore`,
and team is `team-a`, `team-b` or `team-c`. Omit the team segment on `docs` and `chore`
branches that belong to nobody in particular.

### Commits

Small and logically grouped. One commit that does one thing beats one commit that does five.
Present tense, lower case after the colon, no full stop.

```
feat: add TimeSlot overlap and intersect
fix: reject match request to an already connected student
docs: document the matching configuration keys
```

**No AI attribution in commits or pull requests.** Do not add `Co-Authored-By` trailers for an
assistant, "Generated with" footers, or any similar line. The work is submitted as ours and the
history should read that way. If you use an AI assistant, configure it not to add them; this
file is the instruction it should be following.

Never commit `.env`, build output (`target/`, `dist/`, `node_modules/`), IDE folders, or a real
credential in any form.

### Pull requests

Open a PR into `main` as soon as you have something to show; draft it if it is not ready. Cover
four things, all load-bearing:

- **What changed**: enough that a reviewer knows where to look.
- **What it relates to**: link the issue or discussion if there is one.
- **How it was tested**: "ran the app and clicked it" is fine if true. "It compiles" is not.
- **The checks**: no logic in the controller, no entity returned to the API, both authors can
  explain every line, and the empty / invalid / unauthorised cases are handled.

CI runs backend tests and the frontend build on every PR touching those directories. Get it
green before asking for review.

### Reviews

- One reviewer from another team. You learn the codebase by reading it.
- Review against the checks above, not against personal style preference.
- **"Can you explain this line?" is always a fair question.** Both authors must answer it in the
  viva; better to find out now.
- Approve, or say what would make it approvable. "Looks good?" helps nobody.

Merge with **Squash and merge** so `main` keeps one commit per pull request. Delete the branch
after.

---

## When you are asked to build something

1. Be clear what "done" means before you start, and say so in the pull request.
2. Work on a branch, never on `main`.
3. Write unit tests for anything with logic worth testing. Scoring and interval maths are pure
   functions with no excuse for being untested. CI fails a backend with no tests at all, and
   the minimum that satisfies it is also the most valuable test here, a context-load test:

   ```java
   @SpringBootTest
   class ApplicationTests {
       @Test void contextLoads() {}
   }
   ```

   It fails whenever a bean cannot be wired, a config property is missing or an entity mapping
   is malformed. Team B: include it with the scaffold.
4. Handle the empty, invalid and unauthorised cases. A student with no availability, a match
   with no shared courses, a request to view a profile you are not connected to.
5. Stop and ask rather than inventing a requirement. This is a graded specification; guessing
   produces features nobody asked for.

## What not to do

- Do not scaffold or modify `pom.xml` / `package.json` unless the task is explicitly that.
- Do not commit generated build output. `target/`, `dist/` and `node_modules/` are ignored.
- Do not write code you cannot explain. Every author must defend every line in the viva. This
  is assessed.
- Do not leave a TODO in place of a requirement without flagging it in the pull request.
- Do not agree an endpoint with another team without recording it in
  [`docs/API_CONTRACT.md`](docs/API_CONTRACT.md).
