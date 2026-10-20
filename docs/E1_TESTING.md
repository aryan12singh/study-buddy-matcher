# E1 explanation testing and acceptance

Current E1 contract: [API](API_CONTRACT.md#match-explanation-e1),
[decisions](DESIGN_DECISIONS.md#ai-match-explanation-decisions-e1),
[design diagrams](diagrams/E1.md) and [configuration](../README.md#configuration).
Live examples and provider details are in [AI_USAGE.md](AI_USAGE.md#e1-ai-match-explanation-updated-18-october-2026).

## How to verify

From the repository root (needs Docker; creates and deletes its own PostgreSQL and never
touches the shared database):

```bash
./scripts/test-backend.sh '-Dtest=MatchServiceTest,CriterionScorersTest,Explanation*Test,AiExplanationWriterTest,MatchExplanationServiceTest,MatchExplanationHttpTest,MatchingQueryCountTest,GeminiExplanationProviderTest'
```

From `frontend/`:

```bash
npm test -- --reporter=dot src/features/matchexplanation src/features/matching
npm run lint
npm run build
```

Results on 18 October: complete backend suite 567 passed with no failures, errors or skips;
matching and explanation frontend tests 83 passed.

## Before a demo

The free tier allows **20 generate requests per day per project and model**, so a live demo
must not depend on fresh calls.

1. Use the demo database and the presenter account. Check today's remaining requests at
   https://ai.dev/rate-limit.
2. **Warm the cache:** search and click **Explain this match** once on every card you plan to show,
   at least 15 seconds apart. Confirm each shows the **AI** badge.
3. After warming, do not change the prompt version, model, matching weights or the demo students'
   preferences: any of these changes the cache key or the card, and the next click needs a fresh call.
4. On the day, warmed cards answer from the cache without calling Gemini. If a fresh call is
   refused, the student sees the standard explanation with **Try AI again**, never an error.

## Live provider status (18 October)

`gemini-2.5-flash` returned 404 for this project, so the default is now `gemini-3.5-flash`; a direct
request with E1's settings finished normally only with `thinkingBudget: 0`. Through the running app,
the GOOD and FAIR demo cards returned reviewed AI explanations. STRONG timed out at a then 3s
timeout (now 5s), and the COR-STAT1202 card hit the daily limit; the cooldown then answered in
0.4s with the standard text. Record those two once the quota resets.

## Regression map

| Behavior | Main automated evidence |
| --- | --- |
| Search and direct evaluation issue the same ID for unchanged inputs; sorting/limit do not change it | `MatchServiceTest.evaluateScoresExactlyAsSearchDoes`, `cardIdentityIgnoresSortAndLimitButChangesWithConfiguration` |
| Configuration, candidate/searcher preference or availability changes reject an old card with 409; refreshed card succeeds | `MatchExplanationHttpTest.changedScoringInputsRequireARefreshedCard` |
| Missing/malformed identity returns 400; stale identity fails even with AI off and before cache/quota/provider | `MatchExplanationHttpTest`, `MatchExplanationServiceTest` |
| Completed and in-flight answers disappear on a real request-send refresh with a changed evaluation; aborted late replies cannot repopulate them | Parameterized post-send cases in `FindBuddiesPage.test.tsx`; panel rerender/abort tests |
| A relationship-only refresh preserves valid wording; stale 409 offers and performs a card refresh before the next explanation | Matching-page interaction tests and `MatchExplanationHttpTest.requestRelationshipChangesDoNotInvalidateTheScoredCard` |
| An unsaved searched goal is present in evidence with either empty or different saved preferences | Parameterized `MatchServiceTest.evidenceUsesAnUnsavedSearchedGoalJustAsTheScorerDoes` |
| Saved target IS442 may score zero while COR1305 is genuinely shared; evidence keeps their meanings separate | `MatchServiceTest.evidenceDistinguishesTheScoredTargetFromAnotherSharedCourse` |
| Provider evidence excludes names, IDs, contact/profile details, schedules and evaluation identity | `ExplanationEvidenceFactoryTest`, prompt tests and HTTP response privacy checks |
| Exact card identity and rounded semantic cache identity are separate; searched intent stays in cache evidence | `ExplanationHasherTest` |
| Read failure returns standard without spending quota; write failure retains valid AI text; core matching errors propagate | `MatchExplanationServiceTest` read/write/core-error cases |
| Fresh and cached text obey current length and bounded concrete-reference checks | Validator tests and service cache-revalidation cases |
| Provider error, malformed output and slow reply fall back; quota is process-local; duplicate insertion retains one row | `GeminiExplanationProviderTest`, `ExplanationQuotaTest`, `MatchExplanationHttpTest.savingTheSameEvidenceTwiceKeepsOneRow` |
| Hibernate-origin and previously migrated cache schemas converge to one unique index without losing cached data | `legacy-migration-fixture.sql`, `assert-migrated-legacy.sql` and replay in `scripts/test-migrations.sh` |
| Rendering treats returned markup as text, labels source and states that AI text may be inaccurate | `MatchExplanationPanel.test.tsx` |
| Hyphenated course codes (COR-STAT1202) and ordinary acronyms (OOP) are accepted; code names, soft success claims and wrong spelled-out hours are rejected | `ExplanationOutputValidatorTest`, including every seeded course code and recorded live replies |
| Evidence reaches the model in plain words with weights and without the admin strategy; the prompt view cannot drift from the evidence record | `ExplanationPromptBuilderTest`, `ExplanationVocabularyTest` |
| AI wording steps (cache, quota, provider, checks, save) are separate from the freshness check | `AiExplanationWriterTest`, `MatchExplanationServiceTest` |
| A 429 pauses further provider calls for the cooldown without spending the daily allowance | `GeminiExplanationProviderTest.rateLimitReturnsEmptyAndPausesFurtherCalls`, `ExplanationQuotaTest` |
| The panel keeps one polite live region, shows a short AI badge and caution, and offers **Try AI again** after a standard result | `MatchExplanationPanel.test.tsx` |

## Practical limits

Output checks cover JSON, length, success and likelihood wording, leftover code names, course codes
and weekly-hour numbers. They do not verify every sentence, so the panel says AI text may be
inaccurate and the score bars stay visible. Cached text is re-checked on every hit. The quota and
cooldown live in one process and reset on restart; Google's daily limit resets on Pacific time.
Simultaneous identical cache misses may make more than one provider call; insert-on-conflict keeps
one row. The evaluation ID checks freshness, not authorization.
