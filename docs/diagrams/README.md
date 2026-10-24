# Diagram index

Baseline and E2 diagrams for Study Buddy Matcher, checked against revision `9592fd2` on
18 October 2026. They cover the compulsory A/B/C implementation and E2 at that revision.
The E1 provider path on [PR #61](https://github.com/aryan12singh/study-buddy-matcher/pull/61)
has its own companion view and is outside this revision-scoped set. E2's database rollout
is recorded in [database operations](../DATABASE_OPERATIONS.md#e2-rollout--16-october-2026);
diagram completion does not imply external-client, team, device or submission acceptance.

## Presentation and review views

Open the SVG directly for a scalable view or insert it into the slide editor as a vector
image. Keep one panel per slide; do not squeeze every class panel onto one slide.
The architecture and matching sequence provide the main narrative. The class panels
support the OO explanation and Q&A, while the E2 panels explain the selected extension.

| View | Scalable export | Source trace and purpose |
| --- | --- | --- |
| Architecture and privacy | [architecture.svg](exports/architecture.svg) | `security`, feature controllers/services, repositories, `ProfileViewAssembler`, `weather`; browser/API/DB/external-service boundaries |
| Compulsory UML use cases (#19) | [use-cases.svg](exports/use-cases.svg) | Student/Admin capabilities, public entry points and group-scoped leader constraint |
| E2 UML use cases | [e2-use-cases.svg](exports/e2-use-cases.svg) | Accepted members, delegated hosts and buddy/group session authority; one integrated extension |
| Identity/profile classes (#18) | [identity-profile.svg](exports/identity-profile.svg) | `User`, `Student`, `Role`, `Course`, `AvailabilitySlot`; actual `@MapsId` association and multiplicities |
| Relationship/group classes (#18) | [relationship-domain.svg](exports/relationship-domain.svg) | Separate buddy/join request state machines, connections, accepted memberships and per-group leader |
| Matching OO design (#18) | [matching-design.svg](exports/matching-design.svg) | Five `CriterionScorer` implementations, `MatchingStrategy`, `PriorityTierStrategy`, immutable score and service coordination |
| Values and DTO privacy (#18) | [values-and-privacy.svg](exports/values-and-privacy.svg) | `WeeklyAvailability`/`TimeSlot` value composition and the actual sealed `ProfileDto` implementations |
| Matching sequence (#20) | [matching-sequence.svg](exports/matching-sequence.svg) | Invalid/auth/empty paths; source-ordered filtering, scoring, minimum score/quality, ranking, count/limit and safe DTO assembly |
| E2 collaborators | [collaborative-study.svg](exports/collaborative-study.svg) | Principal room/session/weather classes; independent presence, shared state, dated plans and provider boundary |

## Editable sources and rebuild

The presentation exports are generated from one editable source,
[`render_diagrams.py`](render_diagrams.py). Each named function defines one panel's
labels, UML shapes and routing. The renderer uses only the Python 3 standard library;
it adds no Java/npm dependency, remote font, service or network request. Do not edit
generated SVGs directly.

```bash
# From the repository root; any installed Python 3 is sufficient.
python3 docs/diagrams/render_diagrams.py

# Confirm the committed SVGs match their source, parse as XML and name real Java classes.
python3 docs/diagrams/render_diagrams.py --check
```

After a model/API change, update the relevant source panel and the detailed view below,
rebuild, inspect the full SVG at a useful size, and check that labels and associations
remain readable. The recorded source revision is a baseline identifier, not a claim that
the diagram was automatically inferred from every Java declaration. The class-name check
catches missing/renamed classes; it cannot establish semantic correctness or human review.

The SVGs contain native text/shapes, an accessible title/description and no embedded raster
images. A browser can open them directly without a build. PNG/PDF copies may be exported
from the browser or slide editor for a specific presentation size, but the SVG remains
the controlled vector export. Verify any presentation export on the actual projector.

## Detailed supporting views

| Editable document | Contents |
| --- | --- |
| [PLATFORM.md](PLATFORM.md) | Whole-system capability map, layers, E2 boundary, profile/auth domain and platform workflows |
| [TEAM_A.md](TEAM_A.md) | Full matching collaborator view, configuration ER, search sequence and versioned save/restore |
| [TEAM_C.md](TEAM_C.md) | E2 detail, request/privacy/group/admin classes, ER, and transaction/lifecycle sequences |

These Markdown files retain editable Mermaid for detailed review. Their flowcharts are
capability maps rather than formal UML use-case diagrams. The new SVG use-case panels use
actor associations, ellipses and an explicit system boundary. ER diagrams describe storage
and remain separately labeled; they do not replace the OO class diagrams.

## Notation and source constraints

- In class panels, `+` is public, `-` private and `#` protected. Selected fields/methods
  are shown; constructors, ordinary accessors and repeated parameter types may be omitted.
  Record properties stand for immutable components/accessors, not mutable public fields.
- Plain class associations carry endpoint multiplicities. A dashed hollow-triangle arrow
  implements an interface; a solid hollow-triangle arrow extends a class. An ordinary
  arrow indicates a dependency. The filled diamond appears only for contained time values.
- `Student` has a one-to-one association with concrete `User`, sharing its ID via `@MapsId`.
  There is no Student/Admin subclass tree. Group leader is the student referenced by that
  particular group, and the leader also occupies one accepted membership.
- `PublicProfileDto` and `ConnectedProfileDto` independently implement `ProfileDto`; one
  does not inherit the other. Contact disclosure is a server policy, not a display rule.
- The sequence's grouped collaborator lifeline is explicitly labeled. `break` terminates
  the interaction when its guard is true; `loop` scores filtered candidates. Empty success
  is HTTP 200 with an empty list, not an error. E1 must not be presented as a scorer/strategy.
- Room state reads and existing-lease renewals recheck access without a group-row lock.
  Admission and timer/configuration transitions use account-before-group locking.
  Session mutations serialize on their current parent; neither room presence nor a calendar
  snapshot is an attendance promise.

## Verification and remaining acceptance

On 18 October, the presentation sources were checked against the named Java classes and
important signatures, all nine SVGs were rebuilt and XML/class-name/source consistency
checks passed. All nine were rasterized with an already available local SVG renderer and
visually inspected for legible labels, unclipped bounds and routed associations.

The remaining gates for issues #18–20 are a cross-team source walkthrough, confirmation
that both authors can explain the design, selection into the editable deck, and physical
projector readability. No instructor approval, exact LMS deadline, deck submission or
submission receipt is established by these files.
