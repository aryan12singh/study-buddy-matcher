# Team workflow

Milestones and how the team picks up, reviews and merges work. The rules for branches, commits
and pull requests are in [AGENTS.md](../AGENTS.md#git-workflow); this page holds the schedule
and the reasons.

## Milestones

| Milestone | Date | Means |
| --------- | ---- | ----- |
| Base code | 11 Oct | Every compulsory feature works end to end |
| Internal feature freeze | 24 Oct | Feature work stops; tests, integration and polish are ready for delivery |
| Extras/integration delivery target | 25 Oct | Team issue milestone for the two required extras |
| Materials/rehearsal target | 8 Nov | Supporting materials and timed demo/Q&A rehearsal |
| Final handoff target | 15 Nov 2026, 11:59 PM SGT | Team target; verify the actual LMS deadline and submission receipt |

The brief sets the final deadline as Week 13; these dates are the team's planning targets.
Feature freeze is in effect: only fixes, tests, documentation and demo preparation.

## Picking up work

1. Say what you are starting, wherever the team is coordinating. Two people building the same
   screen is the expensive mistake here.
2. Be clear what "done" means before you start: which behaviours, which edge cases.
3. If the requirement is ambiguous, ask. Do not guess at a graded requirement.

## Pull requests

Open a pull request into `main` as soon as you have something to show; draft it if it is not
ready. Cover four things:

- **What changed**: enough that a reviewer knows where to look.
- **What it relates to**: link the issue or discussion if there is one.
- **How it was tested**: "ran the app and clicked it" is fine if true. "It compiles" is not.
- **The checks**: no logic in the controller, no entity returned to the API, both authors can
  explain every line, and the empty, invalid and unauthorised cases are handled.

CI runs the backend tests and the frontend tests, lint and build on every pull request into
`main` and every push to `main`. Get it green before asking for review.

## Reviews

- One reviewer from another team. You learn the codebase by reading it.
- Review against the checks above, not against personal style preference.
- "Can you explain this line?" is always a fair question. Both authors must answer it in the
  viva; better to find out now.
- Approve, or say what would make it approvable. "Looks good?" helps nobody.

Merge with **Squash and merge** so `main` keeps one commit per pull request, then delete the
branch.

## Ownership

Each team stays in its lane (table in [AGENTS.md](../AGENTS.md#team-ownership)). A surprise
refactor in someone else's file is how merge conflicts and "I cannot explain that line" happen:
if another team's code blocks you, raise it rather than rewriting it.
