# Matching engine changes: decision report

Branch `fix/team-a/matching-engine`, eleven commits on top of `main` at `9bccf7b`, local only
(not pushed). Companion seed branch `fix/team-b/demo-seed-matching-variety`, one commit on
top of `feat/team-b/demo-seed-story`. Prepared 15 Oct 2026 for Team A to review, adopt or
adapt. Team A owns the matching engine, so nothing here should merge without Team A agreeing
to own and explain it.

AI disclosure: these changes, tests and this report were produced with an AI coding assistant
(Claude) at a Team C member's request, from a review of `main`. Record that in
[AI_USAGE.md](AI_USAGE.md) if any of it is kept.

## Contents

1. [Summary](#summary)
2. [How to review, keep or revert](#how-to-review-keep-or-revert)
3. [How matching worked before](#how-matching-worked-before)
4. [Decisions, one per commit](#decisions-one-per-commit)
5. [Seed changes](#seed-changes)
6. [What was deliberately not done](#what-was-deliberately-not-done)
7. [Verification evidence](#verification-evidence)
8. [Things the team needs to know or decide](#things-the-team-needs-to-know-or-decide)
9. [Viva notes: explaining each change](#viva-notes-explaining-each-change)

## Summary

| # | Problem on `main` | Change | Commit |
| --- | --- | --- | --- |
| 0 | The brief's Appendix A example was never tested end to end | `AppendixAMatchingTest` reproduces the table: 2-hour overlap, 0.85 Strong | `2c5688e` |
| 1 | Missing preferences scored a fixed 0.5: empty profiles reached Good, and "no timetable" tied with real overlap | Missing data is *unknown*, scores a configured value (0) and shows as "Not shared" | `fc0ba5f` |
| 2 | Availability and goal scores depended on who searched (A→B ≠ B→A) and penalised big timetables | Divide by the smaller timetable / smaller goal set | `260b5e3` |
| 3 | Availability-first and Course-first gave their own criterion weight 0 and sorted on its exact value: "1.00 Strong" ranked below "0.65 Good"; 15 extra minutes outranked everything | Rank by configurable bands first, then score; priority criterion gets the largest default weight | `1ab46f5` |
| 4 | Only one course was ever compared; a goal search scored the goal 1.0 for everyone | Score overlap of all current courses when no course is in focus; goal search also compares saved goals; partial course score moved to config | `cd669ce` |
| 5 | A 15-minute overlap counted as shared time | Only shared blocks of at least 60 minutes (configurable) count | `507c3ee` |
| 6 | `WeightedMatchScorer` scored *and* wrote English; a test-only constructor faked a label | `MatchExplainer` interface + `TemplateMatchExplainer`; the explainer sees only the score | `fdea93e` |
| 7 | Sort options chosen by an if/else on an enum; ties broken alphabetically | Each `MatchSort` value supplies its comparator; ties go to more shared hours, then name, then id | `0f4a1bd` |
| 8 | One search ran 127 SQL statements with 50 students | Entity-graph bulk load: 20 statements, none per candidate, identical results | `b19341f` |
| 9 | Docs described the old rules | README, API contract, design decisions, Team A diagrams, testing guide, coverage | `b7db4f5` |
| 10 | This report | | (this commit) |

Seed branch: the presenter's matches all scored the same on group size and nearly all on
availability, so switching strategy barely changed the list. Two data edits plus a guard
test fix that (`962eab0`).

## How to review, keep or revert

Nothing touched `main`, your existing branches or the shared database. Both branches exist
only in your local repository.

```bash
git log --oneline main..fix/team-a/matching-engine
```

```bash
git diff main...fix/team-a/matching-engine --stat
```

To look at a single decision, `git show <commit>`; each commit compiles and passes the
full test suite on its own. To drop one decision but keep the rest, revert that commit on the
branch (later commits may need a small conflict fix, because several touch the same tests):

```bash
git revert <commit>
```

To discard everything:

```bash
git branch -D fix/team-a/matching-engine fix/team-b/demo-seed-matching-variety
```

The two branches are independent. The seed fix sits on Team B's seed branch; the matching fix
sits on `main`. They merge together without conflicts (checked).

## How matching worked before

A search (`GET /api/matches`) loads the searcher, every active student and every
availability slot, removes self and existing connections, applies hard filters (course or
goal, mode, group size, minimum shared hours), scores each remaining candidate, filters by
minimum score and quality, sorts, and returns the top `limit` as DTOs.

Scoring: five `CriterionScorer` implementations each return 0 to 1; `WeightedMatchScorer`
adds weight × score using the active strategy's weights; the total maps to Strong (≥ 0.75),
Good (≥ 0.50) or Fair. Missing data on either side scored 0.5. Ordering: Balanced sorts by
total; Availability-first sorted by the availability score then total, Course-first by the
course score then total, with those criteria weighted 0 by default.

## Decisions, one per commit

Each entry: what changed, why, the alternatives considered and why they lost, and the risk.

### 0. Lock the brief's example in a test first (`2c5688e`)

**What.** `AppendixAMatchingTest` builds Students A and B from Appendix A (Wed 7–9 pm and
Fri 2–4 pm vs Wed 7–9 pm and Thu 6–8 pm; in person vs either; exam preparation; 2–3 vs 2) and
asserts every row: course match, 2-hour overlap, compatible mode, matching goal, compatible
group size, a 0.85 Strong total under Balanced, and the same total from either side.

**Why first.** It is the only worked example the assignment gives. Writing it before any
change means every later commit is checked against it; it passed on `main` and still passes.
It is also the cleanest answer to "does your engine meet the brief?" in the viva.

### 1. Missing data is unknown, not half marks (`fc0ba5f`)

**What.** Scorers return a `CriterionResult` (a known score, or unknown) instead of a bare
`double`. `WeightedMatchScorer` substitutes `app.matching.scoring.unknown-score` (default 0)
for unknowns and records them in `MatchScore.unknownCriteria`. The breakdown DTO gains
`known`; the card shows "Not shared" instead of "Partly" or "0 h". `NEUTRAL_SCORE` is gone.

**Why.** Every scorer produces a ratio, so 0.5 is not neutral: on the availability scale it
means "free for half your week". Verified on `main`: a student with no timetable tied with a
real 2-of-4-hour overlap and won on alphabetical order, and a profile with only a target
course scored about 0.65, labelled Good. The card also contradicted itself, showing "0 h"
while awarding 0.5 × weight.

**Alternatives rejected.**
- *Renormalise* (drop unknown criteria, average the rest): a profile with one perfect
  criterion would score 1.00 Strong, recreating the bug; it also needs a second rule (a
  completeness cap) to patch that, which is harder to explain.
- *Keep 0.5 but relabel it*: the ranking problem stays.
- *Hard-code 0*: violates the no-magic-numbers rule; a configurable value lets the team
  choose a policy without code changes.

**Risk.** Students with incomplete profiles now rank lower. That is the intent, and the card
says why. No label cap was added: with unknown worth 0, an empty profile cannot reach Strong.

### 2. Symmetric availability and goal scores (`260b5e3`)

**What.** Availability = shared minutes ÷ the *smaller* of the two timetables (was: ÷ the
searcher's). Goals = shared goals ÷ the *smaller* goal set (the overlap coefficient; was: ÷
the searcher's goals).

**Why.** A match is a relationship between two people, and a request is accepted by the
other person, so both should see the same compatibility. The old rule also punished honesty:
a student listing 20 free hours saw a real 4-hour overlap as 0.2. Appendix A is unchanged
(both students have 4 hours, so 2/4 = 0.5).

**Alternatives rejected.**
- *Jaccard (shared ÷ union) for goals*: symmetric, but a student who wants exam prep and
  meets someone open to exam prep, projects and concepts would score 0.33, although their
  need is fully met. The overlap coefficient treats "open to several goals" like choosing
  "either" for study mode, which is how the engine already treats mode.
- *A target-hours scale* (`min(1, shared / 3 h)`): intuitive, but it introduces a new
  requirement the team would need to justify. Kept as a possible future option.

**Test impact.** Two HTTP fixtures had *shortened* one student's slot to get a partial score.
Under the symmetric rule a shorter slot inside a longer one is (correctly) a full match, so
they now *shift* the slot instead, keeping their intended 0.75 and 0.5.

### 3. Priority strategies rank by bands and weight their own criterion (`1ab46f5`)

**What.** New abstract `PriorityTierStrategy` (Template Method). Availability-first and
Course-first now only say which criterion they prioritise and what value it is banded on.
Ranking is: band of the priority criterion first, then the weighted total within the band.
Bands come from `app.matching.priority-tiers.*`: availability `[3.0, 1.0]` hours (3 h or
more, 1 to 3 h, under 1 h) and course `[1.0, 0.5]` (wants a buddy for the course, takes it,
neither). Default weights now give the priority criterion the largest share
(Availability-first: availability 0.40, course 0.25, mode 0.15, goal 0.10, group 0.10;
Course-first mirrors it). `MatchingStrategy` has one `ranking(enabled)` method instead of two.
The strategy descriptions on the admin and results screens now say what the bands are.

**Why.** Reproduced live on `main`: under Availability-first, a student with 0 shared hours
showed "1.00 Strong" yet ranked 4th below three "0.65 Good" cards, because the order used one
number (availability) and the card showed another (a total that ignored availability). And
because the sort used the exact availability value, 0.76 always beat 0.75 regardless of course,
mode or goals.

**Alternatives rejected.**
- *Weight presets only* (Availability-first = a heavier availability weight, sort by total):
  simplest, but it does not "prioritise students with the strongest timetable overlap before
  considering other criteria" as Appendix A describes.
- *Keep strict ordering, just give the criterion a weight*: fixes the label, not the
  hairline effect.
- *Feasibility gates* (no overlap = not a match): changes the product; not in the brief.

**Why these band values.** One hour is the shortest useful session (same as decision 5);
three hours is more than one session a week. Appendix A's 2-hour overlap falls in the middle
band. Course bands match the three values the course scorer can produce in a course search.
All are configurable.

**Risk.** Within the top band, a card with a lower score can appear above a card from a lower
band with a higher score, for example 6 h at 0.775 above 2 h at 0.800. That is the point of
the strategy, and the results header now explains the grouping. Saved overrides in an existing
database keep their old weights until an admin restores defaults (see "Things the team needs
to know").

### 4. Compare all shared courses and goals (`cd669ce`)

**What.** With no course in focus (goal-only search and no saved target), the course score is
shared courses ÷ the smaller course list instead of 0.5 for everyone. A goal search adds the
searched goal to the searcher's saved goals instead of replacing them. The "takes the course
but wants a buddy for another" score moved from a constant to
`app.matching.scoring.takes-course-score` (0.5).

**Why.** The brief's objective is matching "based on shared courses", plural; `main` compared
one course at most. After the goal filter, every candidate already had the searched goal, so
the goal criterion gave everyone 1.0 and its weight was a flat bonus that changed nothing but
inflated labels. Moving 0.5 to config fixes a breach of the team's no-magic-numbers rule.

**Unchanged on purpose.** With a course in focus the three-level course rule stays, because it
distinguishes "wants a buddy for this course" from "only enrolled", which the team designed.

### 5. Ignore overlaps too short for a session (`507c3ee`)

**What.** Only shared blocks of at least `app.matching.scoring.min-session-minutes` (60)
count, in the availability score, the shared-hours figure, the minimum-hours filter and the
shared-slot list. `WeeklyAvailability` keeps its raw interval maths (`sharedMinutesWith`) and
gains separately named `sharedSessionsWith` / `sessionMinutesWith`, so the two meanings never mix.

**Why.** A 15-minute overlap cannot hold a study session, so counting it overstates
compatibility.

**Alternatives rejected.** Changing the existing methods' meaning would have silently changed
every caller and test of the interval maths. A separate `SharedAvailability` bean would have
created a package cycle (availability → scoring → availability).

### 6. Explanations behind an interface (`fdea93e`)

**What.** `MatchExplainer` (one method: `explain(score, enabled)`) with `TemplateMatchExplainer`
holding the existing wording, called from `MatchResultAssembler`. `MatchScore` no longer
carries the sentence. The test-only `score(...)` overload and the two-argument `MatchScore`
constructor that faked a Fair label are removed.

**Why.** Single responsibility: scoring numbers and writing English change for different
reasons. It is also the seam the E1 AI match explanation needs: an LLM-backed explainer can be
a second implementation chosen by configuration, with the template as the default and offline
fallback. Because the explainer receives only the score, never a `Student`, it cannot leak a
contact number or other private field: privacy by construction, which is a strong
responsible-AI argument.

### 7. Sort options with behaviour, meaningful ties (`0f4a1bd`)

**What.** Each `MatchSort` value implements `before(searcher)`, the comparator it applies
before the strategy (Best match compares nothing first). `MatchService.ordering` is now one
chain: sort, strategy, more shared hours, name, id. `ScoredCandidate` is a package-level record.

**Why.** AGENTS.md prefers an interface or polymorphism over a switch on a type; adding a sort
option now touches one place. Ties used to go to alphabetical order, which is how "Demo
Student 32" beat "Demo Student 43" on `main`; more shared hours is a tie-break a student cares
about. Name and id stay last so the order is still deterministic.

### 8. Bounded query count (`b19341f`)

**What.** `StudentRepository.findWithMatchingDetailsByIdIn` loads candidates with courses,
goals and target course through an `@EntityGraph`;
`AvailabilitySlotRepository.findByStudentIdIn` loads slots for eligible students only (was:
every slot in the database, including inactive students').

**Measured** with the 50-student demo seed against PostgreSQL statement logging: one IS442
search went from **127 to 20 statements** (about 12 queries plus transaction control), with
identical results. None of the remaining queries runs per candidate.

**Why.** Over Supabase on campus wifi, 127 round trips can take seconds, and Home calls the
same endpoint. The query count is also a good before/after slide.

**Overlap with another branch.** A separate local branch, `fix/team-a/match-search-query-count`,
solves the same problem globally with `hibernate.default_batch_fetch_size` and adds
`MatchingQueryCountTest`. That test also passes on this branch. The approaches touch different
lines and can coexist; pick one owner. This branch's approach is targeted (only the search path
changes); the other is one line but affects every entity. Keeping that branch's test as a
guard is recommended either way.

### 9. Documentation (`b7db4f5`)

README scoring table and config rows, API contract (scoring, strategies, tie-breaks, `known`
field), design decisions (rationale rows), Team A class and sequence diagrams (new classes:
`CriterionResult`, `ScoringRules`, `PriorityTierStrategy`, `PriorityTiers`, `MatchExplainer`,
`TemplateMatchExplainer`, `MatchSort` behaviour), the Team A testing guide's worked example
(now 0.75 because missing group size is unknown) and requirement coverage evidence. Each new
config key also got its README row in the commit that added it, as AGENTS.md requires.

## Seed changes

Branch `fix/team-b/demo-seed-matching-variety` (one commit, `962eab0`) on top of Team B's
`feat/team-b/demo-seed-story`, which already rebuilt the seed well: 50 named students across
five schools, 14 courses, every preference category, three students without a timetable, the
Appendix A pair (Adele Ang and Ben Boon), requests in every state, four groups and a
deactivated account. That branch is checked out in another worktree, so it was left untouched.

**Problem found.** With the matching fixes, the presenter Priya's IS442 list barely changed
between strategies: she chose "either" for group size so every candidate scored 1.0, and almost
every candidate shared exactly one 2-hour block, so eight tied at 85%. Switching strategy in a
demo would look like it does nothing.

**Change.** Priya now prefers a small group (consistent with the small group she leads), so
one-to-one students score 0 on group size. Chen Xin Yi now shares all six of Priya's free hours
but only takes IS442, so she is 8th under Balanced, 2nd under Availability-first and drops out of
the top 8 under Course-first. Priya's mode stays "either" so her seeded requests still make
sense. A seed test asserts the presenter's matches differ on course, availability, goals and
group size and include a high-overlap, take-only candidate. It fails on the original data
("GROUP_SIZE scores the same for every match") and passes with the change. The README's
presenter row now includes the strategy demo script.

## What was deliberately not done

| Item | Why not |
| --- | --- |
| Related or equivalent courses (`CourseRelation`) | You asked to leave it out for now |
| Separate `MatchingProfile` from the `Student` entity | Purer domain, but it touches every scorer and the filter for little marks value; more for Team A to defend in the viva. The query fix removed its practical cost |
| Admin-screen fields for the new settings (unknown score, bands, session length) | They are externalised in `application.yml`, which meets the brief; making them admin-editable needs a database migration a week before the freeze |
| Learning style as a criterion | The team's recorded interpretation stands. Q&A answer: it would be one new `CriterionScorer`, one enum value and one weight row, with no change to existing scorers |
| Privacy of shared slots in match results | Strangers can see the exact shared time blocks. Not a brief breach (only the contact number is protected) but a team decision; record it in DESIGN_DECISIONS. The new explainer sees only hour counts |
| Quality thresholds (0.75 / 0.50) | Left alone. With the seed, most of Priya's IS442 matches are Strong because they genuinely share the course, mode and goals; only incomplete or mismatched profiles fall to Good. Whether Strong should be rarer is a team call (raise `app.matching.quality.strong`), not a correctness fix |
| Visual browser check of the UI | It needed signing in to the demo app; the UI changes (one label, two descriptions) are covered by the type-check and component tests instead |

## Verification evidence

- Backend: **398 tests, 0 failures** on the branch tip, against PostgreSQL 18 with all
  migrations applied (CI uses PostgreSQL 17). Every commit was run through the full suite before
  committing. Counts come from the Surefire XML reports, which include `@Nested` classes.
- Frontend: type-check, **274 tests**, lint (only the existing `OwnerProfilePage` warning) and
  production build all pass.
- Combined check: the seed story + seed fix + matching fixes merged cleanly in a scratch
  checkout and passed **406 backend tests**. The live demo then showed, for Priya's IS442 search:

| Strategy | Top of the list |
| --- | --- |
| Balanced | Marcus 0.95 (4 h), then four at 0.85 / 2 h, then Hui Min and Meera 0.80, Chen Xin Yi 0.75 (6 h) 8th |
| Availability first | Marcus (4 h band), Chen Xin Yi (6 h band) 2nd, then the 2 h band by score |
| Course first | All "wants a buddy for IS442" students by score; Chen Xin Yi (takes only) out of the top 8 |

  Within every band the displayed score never increases down the list, and Kai (no timetable)
  shows availability "Not shared" and sits near the bottom instead of tying with real overlap.
- Query count: 127 → 20 statements per search (method above).

## Things the team needs to know or decide

1. **Ownership.** All of this is Team A's area. Team A should review commit by commit and
   either merge it under their names or reimplement what they agree with. Every author must
   be able to explain every line.
2. **Team B files touched.** One finder method each in `StudentRepository` and
   `AvailabilitySlotRepository`; nothing existing changed. The seed fix is on its own branch on
   top of Team B's.
3. **Restore defaults once after merging.** Matching settings an admin already saved in the
   shared database keep their old weights (priority criterion 0). The bands still fix the
   ordering, but the score will ignore the priority criterion until an admin clicks "Restore
   defaults" or re-weights it.
4. **Duplicate query-count work.** Choose between this branch's entity graph and
   `fix/team-a/match-search-query-count`'s batch size, and keep its `MatchingQueryCountTest`.
5. **Unknown score above 0.5.** If anyone raises `unknown-score` above 0.5, an unknown course
   would land in Course-first's middle band. Keep it at 0 unless the team decides otherwise.
6. **Shared-slot privacy.** Decide whether strangers should see exact shared times, and record
   it.
7. **E1.** Build the AI explanation as a second `MatchExplainer`; keep the template as the
   default so the app works without a key or network.

## Viva notes: explaining each change

- *Why is missing data worth 0 and not 0.5?* Every score is a ratio, so 0.5 means "half
  compatible", not "unknown". Zero plus a "Not shared" label means missing data never beats real
  data, and the value is configurable.
- *Why divide by the smaller timetable?* A match is mutual; both students should see the same
  number, and listing more free time should not count against you.
- *What does Availability-first actually do?* Groups students into shared-time bands and ranks
  by overall score inside each band. That follows Appendix A's "prioritise overlap before other
  criteria" without letting a few minutes outweigh everything.
- *Which design patterns are here?* Strategy (`MatchingStrategy`, `MatchExplainer`), Template
  Method (`PriorityTierStrategy`), enum with behaviour (`MatchSort`), immutable value objects
  (`CriterionResult`, `ScoringRules`, `PriorityTiers`, `MatchScore`), externalised configuration
  bound to typed records.
- *How do you know it meets the brief?* `AppendixAMatchingTest` reproduces the brief's own
  example and gets 2 hours of overlap and 0.85 Strong.
- *How fast is a search?* A fixed number of queries per search regardless of the number of
  students: 20 statements with the 50-student seed, down from 127.
