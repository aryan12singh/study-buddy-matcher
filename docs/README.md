# Documentation index

Every document in this folder, grouped by the question it answers. File names are
`UPPER_SNAKE_CASE.md`; diagram exports and assets are `lower-kebab-case`. Start with the
project [README](../README.md) for the overview and setup.

## What was built, and does it meet the brief?

| Document | Contents |
| --- | --- |
| [REQUIREMENT_COVERAGE.md](REQUIREMENT_COVERAGE.md) | Every requirement in the brief and both extras, mapped to the classes and tests that deliver it |
| [LIMITATIONS_AND_FUTURE_WORK.md](LIMITATIONS_AND_FUTURE_WORK.md) | Deliberate boundaries of the build and how the design extends past them |
| [AI_USAGE.md](AI_USAGE.md) | AI inside the app (what Gemini receives, safeguards) and AI tools used to build the project |

## How is it designed?

| Document | Contents |
| --- | --- |
| [DESIGN_DECISIONS.md](DESIGN_DECISIONS.md) | The team's choices where the brief is open, with the reason and the alternative for each |
| [diagrams/README.md](diagrams/README.md) | Index of the 21 diagram panels, the notation used and how to rebuild them |
| [diagrams/SYSTEM_OVERVIEW_DIAGRAMS.md](diagrams/SYSTEM_OVERVIEW_DIAGRAMS.md) | Whole system: capability map, runtime layers, external services, identity and profile, sign-in flows |
| [diagrams/MATCHING_ENGINE_DIAGRAMS.md](diagrams/MATCHING_ENGINE_DIAGRAMS.md) | Team A: scorer and strategy classes, matching configuration tables, search and save sequences |
| [diagrams/AI_STUDY_TOOLS_DIAGRAMS.md](diagrams/AI_STUDY_TOOLS_DIAGRAMS.md) | Team B: Quiz Buddy, study plans and Pixel on the shared AI client; quiz lifecycle and sequence |
| [diagrams/CONNECTIONS_GROUPS_AND_ROOMS_DIAGRAMS.md](diagrams/CONNECTIONS_GROUPS_AND_ROOMS_DIAGRAMS.md) | Team C: requests, connections, groups and notifications (state machines, sequences, ER) and the E2 rooms, forest and sessions |
| [diagrams/MATCH_EXPLANATION_DIAGRAMS.md](diagrams/MATCH_EXPLANATION_DIAGRAMS.md) | E1 match explanation: freshness, evidence, cache, quota and the Gemini call |
| [diagrams/exports/](diagrams/exports/) | The diagram panels as SVG files, ready for slides |

## How do I use, run or change it?

| Document | Contents |
| --- | --- |
| [API_CONTRACT.md](API_CONTRACT.md) | Every endpoint: request and response shapes, authorisation and errors |
| [CONFIGURATION.md](CONFIGURATION.md) | Every configuration key with its default and purpose |
| [DATABASE.md](DATABASE.md) | Access model, migrations, schema overview, demo seed and reset, release runbook |
| [TESTING.md](TESTING.md) | Test strategy, how to run each suite, coverage by area and the demo walkthroughs |

## How does the team work?

| Document | Contents |
| --- | --- |
| [../AGENTS.md](../AGENTS.md) | Rules for contributors and coding assistants: the two layering rules, conventions, secrets, git workflow |
| [UI_GUIDE.md](UI_GUIDE.md) | Visual direction, Figma link and the shared React components to reuse |
| [TEAM_WORKFLOW.md](TEAM_WORKFLOW.md) | Milestones, picking up work, pull requests and reviews |

## Assets

| Folder | Contents |
| --- | --- |
| [assets/screenshots/](assets/screenshots/) | Screens used in the README |
| [assets/icons/](assets/icons/) | Original pixel icons used in the README |
| [assets/video/](assets/video/) | The launch film and the walkthrough (1080p) |
