# E1 explanation design: historical summary

Originally 14 October 2026. Owner: Charlize (Team C). Issue:
[E1 #15](https://github.com/aryan12singh/study-buddy-matcher/issues/15).
Superseded by the current [API contract](../../API_CONTRACT.md#match-explanation-e1),
[decisions](../../DESIGN_DECISIONS.md#ai-match-explanation-decisions-e1) and
[E1 test guide](../../E1_TESTING.md). The original design remains in Git at
[`5377900`](https://github.com/aryan12singh/study-buddy-matcher/blob/537790018c1a4f97b07266c98aceb73c8389a8ad/docs/superpowers/specs/2026-10-14-e1-ai-match-explanation-design.md).

The retained design intent is short, optional Gemini wording of an existing match's real
score evidence. It does not generate matches, change scores/ranking, expose profiles to the
provider or add AI chat/roadmaps. It uses the existing Spring HTTP client and frontend API
client, constructor injection, DTO boundaries and no new dependency.

The original design's dependencies were Team A's direct evaluation method and matching-card
integration. Those additions are implemented on the E1 branch. Current evaluation checks
also cover scoring profiles, availability, settings and output, so later changes cannot be
explained beside an older card. No persistent search session is required.

Remaining product acceptance is demonstrated provider output against intended evidence,
team review/author understanding and demo integration. Automated fallback/contract tests do
not establish live AI success or grading acceptance. The current test guide owns the record;
this historical summary is not another specification to keep in sync.
