# Diagram index

The diagram panels for Study Buddy Matcher and the pipeline that generates them. Each SVG in
[`exports/`](exports/) is a scalable, self-contained vector panel for slides or review. The
[design diagrams](../DESIGN_DIAGRAMS.md) document walks through the whole design, pairing each
panel with the detailed, editable Mermaid views behind it.

## Exported panels

### System views

| Panel | What it shows |
| --- | --- |
| [tech-stack.svg](exports/tech-stack.svg) | Every library, tool and hosted service with its brand mark, grouped by tier: browser, Spring Boot API, external services, database, and the frontend and backend tooling |
| [architecture.svg](exports/architecture.svg) | Browser, the Spring layers top to bottom (security, controllers, services, entities, matching, client interfaces, repositories), the DTO response path, configuration, error handling, PostgreSQL and the external services, with the contact-privacy boundary |
| [package-layers.svg](exports/package-layers.svg) | UML package diagram: the 25 packages under `com.studybuddy` grouped by owning team, and one feature package opened up to show controller, service, entity, repository, assembler and DTO with the two layering rules |
| [deployment.svg](exports/deployment.svg) | UML deployment diagram: client browser, application host (JVM and Vite), database host, external services and the CI runner, with the protocol on each communication path |
| [database-core.svg](exports/database-core.svg) | Entity-relationship diagram (crow's-foot) of the 17 tables behind the compulsory features |
| [database-extras.svg](exports/database-extras.svg) | Entity-relationship diagram of the 17 tables added by the two extras: rooms, sessions, forest, badges, explanation cache, study plans and quizzes |

### Use cases

| Panel | What it shows |
| --- | --- |
| [use-cases.svg](exports/use-cases.svg) | UML use cases for the compulsory requirements: Student, Study Group Leader (a kind of Student) and System Administrator, with the optional match explanation as an «extend» |
| [e2-use-cases.svg](exports/e2-use-cases.svg) | UML use cases for collaborative study (E2): rooms, Pomodoro, room reactions, the focus forest, music, dated sessions, mutual availability and weather |

### Class diagrams

| Panel | What it shows |
| --- | --- |
| [identity-profile.svg](exports/identity-profile.svg) | `User`, `Student`, `Role`, `Course` and `AvailabilitySlot`, with the `@MapsId` composition (the account owns the profile) and multiplicities |
| [matching-design.svg](exports/matching-design.svg) | The five `CriterionScorer` implementations, `MatchingStrategy`, the abstract `PriorityTierStrategy` template method and its two subclasses, `MatchScore` and the coordinating services |
| [values-and-privacy.svg](exports/values-and-privacy.svg) | `WeeklyAvailability` / `TimeSlot` value composition and the sealed `ProfileDto` response shapes chosen by `ProfileViewAssembler` |
| [relationship-domain.svg](exports/relationship-domain.svg) | `MatchRequest`, `Connection`, `StudyGroup`, `GroupMembership`, `GroupJoinRequest`, their status enumerations and `GroupClosure`, with one association per field |
| [collaborative-study.svg](exports/collaborative-study.svg) | E2 collaborators: `StudyRoomService`, `RoomReactionService`, `ForestService`, `StudySessionService`, `WeatherService` and their domain and value classes |
| [ai-study-tools.svg](exports/ai-study-tools.svg) | Quizzes, personal and session study plans, and Pixel: creation services, writers, reply validators, the rules-first intent reading and the shared `AiClient` / `GeminiClient` / `AiQuota` |

### Sequence diagrams and state machines

| Panel | What it shows |
| --- | --- |
| [matching-sequence.svg](exports/matching-sequence.svg) | `MatchService.search`: account check, settings snapshot, the scoring loop through `CriterionScorer`, threshold, ranking and DTO assembly |
| [matching-config-sequence.svg](exports/matching-config-sequence.svg) | `MatchingConfigService.update` with its version check, then the next search reading the snapshot and ranking through the chosen `MatchingStrategy` |
| [match-request-sequence.svg](exports/match-request-sequence.svg) | `MatchRequestService.send` and `accept`: locks, the new `MatchRequest`, `respond(ACCEPTED)`, the new `Connection` and both notifications |
| [contact-privacy-sequence.svg](exports/contact-privacy-sequence.svg) | `ProfileViewService.view`: `ProfileViewAssembler` creates a `ConnectedProfileDto` or a `PublicProfileDto` from the relationship state |
| [group-join-sequence.svg](exports/group-join-sequence.svg) | `GroupJoinRequestService.accept`: leader check, capacity check, `respond(ACCEPTED)` and the new `GroupMembership` |
| [request-lifecycles.svg](exports/request-lifecycles.svg) | UML state machines for `MatchRequest`, `Connection`, `GroupJoinRequest` and `StudyGroup`, labelled event [guard] / action |
| [e1-explanation.svg](exports/e1-explanation.svg) | E1 match explanation: freshness check, evidence reduction, cache, quota, Gemini call and the AI / STANDARD / problem outcomes |

For a twelve-minute presentation, the technology stack, the architecture panel, one class
panel per feature area and one sequence diagram carry the narrative. Use one panel per slide.

## Rebuilding the exports

Twenty panels are generated by [`render_diagrams.py`](render_diagrams.py), using only the
Python 3 standard library. The diagram source is split by kind:

| File | Contents |
| --- | --- |
| [`diagram_canvas.py`](diagram_canvas.py) | The drawing surface: palette, text, UML shapes (class, package, node, state, table), markers and the `Sequence` helper |
| [`baseline_panels.py`](baseline_panels.py) | Architecture, use cases, class and collaborator panels |
| [`structure_panels.py`](structure_panels.py) | Technology stack, packages, deployment and the two database panels |
| [`behaviour_panels.py`](behaviour_panels.py) | Sequence diagrams and state machines |
| [`brand_icons.py`](brand_icons.py) | Brand mark paths for the technology stack panel |

Each named function defines one panel's labels, shapes and coordinates. Edit the source, not
the generated SVGs.

```bash
# From the repository root: rewrite the twenty generated SVGs in docs/diagrams/exports/
python3 docs/diagrams/render_diagrams.py

# Check only: fail if any committed SVG differs from the script's output, and confirm every
# class box, lifeline and table names a Java class or a table in the SQL migrations
python3 docs/diagrams/render_diagrams.py --check

# Optional: PNG copies at twice the size, written to docs/diagrams/exports/png/ (git-ignored)
CHROME=/path/to/chrome docs/diagrams/export_png.sh
```

`--check` writes nothing. It exits with status 1 and lists stale or missing files when an
export is out of date, and raises an error if a panel names a class or table that no longer
exists. It cannot judge whether a diagram is semantically correct, so review the rendered
panel after any model or API change.

`e1-explanation.svg` is not generated by the script: it is a standalone, hand-edited SVG and
`--check` does not cover it. Edit it directly and keep it consistent with the
[match explanations section](../DESIGN_DIAGRAMS.md#match-explanations) of the design diagrams.

All SVGs use native text and shapes with an accessible title and description, and no
embedded raster images, so a browser opens them directly. `export_png.sh` needs only a Chrome
or Chromium binary; the PNG copies are build output and are not committed.

## Brand marks

The technology stack panel draws each technology's mark from
[Simple Icons](https://simpleicons.org) 16.37.0, which publishes the paths under
[CC0 1.0](https://github.com/simple-icons/simple-icons/blob/develop/LICENSE.md). The paths
are copied into [`brand_icons.py`](brand_icons.py), so nothing is downloaded when the panels
are rebuilt and no dependency is added. The marks remain trademarks of their owners and are
used only to name the technologies the project depends on. Freesound, Open-Meteo and Mockito
have no mark in that set: the first two are drawn with a neutral glyph and Mockito shares the
JUnit tile.

## Design document

[DESIGN_DIAGRAMS.md](../DESIGN_DIAGRAMS.md) is the one document that uses these panels. It is
organised by the application: system overview, core requirements, the two extras, then data and
behaviour. Its Markdown flowcharts are capability maps rather than formal UML use-case diagrams;
the SVG use-case panels use actors, associations, ellipses and a system boundary.

## Notation

- **Use cases.** Stick figure = actor, ellipse = use case, rectangle = system boundary. The
  Study Group Leader actor specialises Student (hollow triangle), because a leader is the
  student a group references; the code has no leader subclass or global leader role.
- **Classes.** `+` is public, `-` private and `#` protected, written `name : Type`. Selected
  fields and methods are shown; constructors and ordinary accessors are omitted. Record
  properties stand for immutable components. An italic name is an abstract class and an italic
  operation is abstract.
- **Class relationships.** A solid line with an open arrowhead is an association, navigable
  from the class that holds the field, with a multiplicity at each end. A dashed line with a
  hollow triangle implements an interface; a solid line with a hollow triangle extends a
  class. A dashed open arrow is a dependency. The filled diamond appears only for contained
  time values.
- **Sequences.** A box is a participant (`:ClassName` or `name:ClassName`), the dashed line
  its lifeline and the narrow bar an activation. A solid line with a filled arrowhead is a
  call; a dashed line with an open arrowhead is a return. `alt` and `loop` frames carry their
  guard in brackets, and a «create» message ends at the new object's box. Messages are
  numbered in the order of the calls in the named service method.
- **State machines.** Filled dot = initial state, bullseye = final state, and each transition
  reads `event [guard] / action`.
- **Database.** Crow's-foot notation: two bars = exactly one, circle and bar = zero or one,
  crow's foot = many. `PK`, `FK` and `UQ` mark primary, foreign and unique keys.
- `Student` has a one-to-one association with `User`, sharing its ID via `@MapsId`; there is
  no Student/Admin subclass tree. `PublicProfileDto` and `ConnectedProfileDto` each implement
  `ProfileDto`; neither inherits the other. Contact disclosure is a server policy, not a
  display rule.
- Room state reads and lease renewals recheck access without a group-row lock; admission and
  timer/configuration changes lock accounts before the group. Session changes serialise on
  their parent. Neither room presence nor a calendar snapshot is an attendance promise.
