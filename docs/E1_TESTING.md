# E1 explanation testing and acceptance

Current E1 contract: [API](API_CONTRACT.md#match-explanation-e1),
[decisions](DESIGN_DECISIONS.md#ai-match-explanation-decisions-e1) and
[configuration](../README.md#configuration). The historical plan/spec are summaries only.

## Automated verification: 18 October 2026

The evaluation/cache/lifecycle corrections were checked locally with Java 21, an isolated
PostgreSQL 17 container and the existing frontend toolchain. No shared Supabase database or
live Gemini generation was used for these checks. Provider tests use a loopback HTTP server.

| Check | Observed result |
| --- | --- |
| Selected matching/explanation backend tests, including PostgreSQL HTTP and query-count cases | 102 passed; no failures/errors/skips |
| Fresh/repeated/legacy migration rehearsal performed by the same backend harness | Passed |
| Explanation panel and matching-page interaction tests | 47 passed in two files |
| Frontend lint | Passed |
| TypeScript and Vite production build | Passed |

These are targeted correction checks, not an assertion that the complete suite, every browser,
latest Figma design or the live demonstration account has been accepted.

### Full suite and integrated checks

The subsequent complete branch checks used synthetic accounts and disposable databases:

| Check | Observed result |
| --- | --- |
| Complete `scripts/test-backend.sh` | 531 passed; zero failures/errors/skips, with migration rehearsal |
| Cache-schema correction after the complete run | Migration upgrade/replay and all 7 `MatchExplanationHttpTest` cases passed. The Hibernate-origin fixture retains its cached row, converges to one named unique constraint/index and rejects a duplicate hash |
| Complete frontend `npm test -- --maxWorkers=2` | 332 passed across 37 files |
| Frontend lint, TypeScript and production build | Passed |
| Real packaged app and private PostgreSQL | 14 runtime checks passed, including missing/stale/unsigned requests, profile/configuration drift, optional cache-read failure and the three bounded provider/fallback attempts described below |
| Real Chromium with the app | 7 checks passed: explanation display, 360/390 px containment, another client's profile edit, stale-card recovery and a fresh explanation without reloading the page; no page exceptions |

The initial local frontend runs under concurrent Java/browser load exceeded asynchronous
test timeouts. The affected two files passed unchanged in isolation (48 tests), and the
complete unchanged suite passed on an idle run. No timeout increase, test deletion or CI
job was added. All owned browsers, app servers and disposable containers were removed.

### Bounded live-provider check

On 18 October, a metadata-only request for the configured `gemini-2.5-flash` returned
HTTP 200 and advertised `generateContent`. Exactly three generation attempts then went
through the actual application, with distinct synthetic evidence:

| Evidence | Provider result | Application result |
| --- | --- | --- |
| STRONG: total 1.0, 4 shared weekly hours | HTTP 404 (`NotFound`) | HTTP 200, `source: STANDARD`, matching card identity retained |
| GOOD: total 0.7, 0 shared weekly hours | HTTP 404 (`NotFound`) | HTTP 200, `source: STANDARD`, matching card identity retained |
| FAIR: total 0.4, 0 shared weekly hours | HTTP 404 (`NotFound`) | HTTP 200, `source: STANDARD`, matching card identity retained |

These checks establish working fallback, not successful AI generation. No provider answer
was accepted or cached as AI, and no further generation retries were made. The key remained
outside tracked source and was neither logged nor sent to the browser.

Google's [model lifecycle guidance](https://ai.google.dev/gemini-api/docs/deprecations)
states that access to Gemini 2.5 Flash is limited to projects with prior active use. This is
a possible explanation for metadata success with generation failure, not a verified diagnosis
for the tested account. The provider's loopback test confirms the configured API path,
key header and request JSON. The account owner must verify inference access and the selected
model using Google's [troubleshooting guidance](https://ai.google.dev/gemini-api/docs/troubleshooting),
then record reviewed live STRONG/GOOD/FAIR outputs before marking provider acceptance complete.
Changing model families also requires checking their generation/thinking configuration;
metadata visibility alone is insufficient.

### Model switch and readable wording: 18 October

The 404s above were the model, not the network: Google limits `gemini-2.5-flash` to projects
with prior use. A direct generation request for `gemini-3.5-flash` with E1's exact settings
(JSON reply, `maxOutputTokens: 200`, `thinkingBudget: 0`) finished with `STOP` and a complete
explanation (54 reply tokens). Without `thinkingBudget: 0` the same request stopped at
`MAX_TOKENS` after 189 thinking tokens, so disabling thinking remains required. The default model
is now `gemini-3.5-flash`.

Prompt v4 sends readable values (`ExplanationVocabulary`: "exam preparation", "meeting mode",
"one to one") instead of enum names, and the validator rejects code-style words such as
`ONE_TO_ONE` or `STRONG`, so they fall back rather than reach the screen. Six live v4 samples
contained no code-style words; one draft rule ("lowercase words") made sentences start in
lower case and was reworded to require normal capitalisation. About 13 calls within a few
minutes then returned HTTP 429 (per-minute free-tier limit); the application falls back to the
standard explanation in that case. Reviewed STRONG/GOOD/FAIR examples from the running app are
still to be recorded in `AI_USAGE.md`.

Run from the repository root:

```bash
./scripts/test-backend.sh '-Dtest=MatchServiceTest,CriterionScorersTest,Explanation*Test,MatchExplanationServiceTest,MatchExplanationHttpTest,MatchingQueryCountTest,GeminiExplanationProviderTest'
```

The script creates and deletes its own PostgreSQL container, applies canonical migrations and
refuses the shared project database for destructive HTTP fixtures. Do not substitute a shared
connection or enable the provider in the test profile.

Run from `frontend/`:

```bash
npm test -- --reporter=dot src/features/matchexplanation/MatchExplanationPanel.test.tsx src/features/matching/FindBuddiesPage.test.tsx
npm run lint
npm run build
```

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

## Browser and live-provider acceptance

Use synthetic accounts and the intended configured model. Keep the provider key in ignored
local configuration or the process environment, and never capture it in screenshots/logs.
A live smoke test should be explicitly bounded; repeated cache hits do not prove new provider
success.

1. Search a valid course/goal and confirm no explanation request occurs before clicking.
   Click **Explain this match**; check loading, resulting text, source label and unchanged bars.
2. With AI disabled, verify the returned `STANDARD` text matches the card's deterministic
   explanation and the card's `evaluationId`. Ordinary matching must remain usable.
3. Keep a card visible and alter matching settings or one student's scoring preferences in
   another session. Click Explain: expect 409 `STALE_MATCH_EVALUATION`, no old answer, and a
   working **Refresh matches** action. Request again only after the current card appears.
4. While an answer is complete or delayed, send a match request and let the actual nonpurging
   refresh finish. If the evaluation changed, old text must disappear. If only request state
   changed, valid text should stay. Delayed old responses must not overwrite the new panel.
5. On a miss with AI enabled, record intended STRONG/GOOD/FAIR evidence and each actual result
   (`AI` or `STANDARD`). Compare course provenance, unsaved searched goals, known/disabled
   criteria and approximate weekly hours against the evidence. Verify outbound JSON contains
   only the allowlist. Do not present a standard fallback as a successful AI generation.
6. Recheck keyboard access, narrow layout and readability in the integrated app. Provider
   acceptance and all-screen Figma/projector acceptance are separate from interaction tests.

The dated record above distinguishes successful integration/fallback checks from the still
pending live AI examples. Record subsequent authorized provider acceptance with its date,
configured model and limits when observed.
Cross-team review, both authors' walkthrough, merge and coursework submission remain separate
gates.

## Practical limits

Output validation checks JSON, length, obvious success/percentage language, code-shaped course
references and numeric weekly-hour references. It does not verify every sentence or prevent
all hallucinations; the UI asks students to check the score breakdown. The semantic cache
rounds scores/hours and revalidates stored text against the current policy. Invalid cached
text falls back without generation; no automatic repair of that row is claimed.

The quota applies to one running process and resets on restart. Simultaneous identical cache
misses may make more than one provider call, although insert-on-conflict prevents duplicate
rows. The evaluation ID checks freshness and does not add discovery-membership authorization.
The existing direct active/non-self candidate contract is retained.
