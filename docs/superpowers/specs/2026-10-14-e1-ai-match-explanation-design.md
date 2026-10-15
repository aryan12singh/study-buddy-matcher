# E1 AI match explanation: design

14 October 2026. Owner: Charlize (Team C). Issue: [E1 #15](https://github.com/aryan12singh/study-buddy-matcher/issues/15).
Status: draft for review. Not to be implemented until Team A agrees the dependency in
[Team A dependency](#team-a-dependency).

## Goal

A student looking at a match can click **Explain this match** and read a short, plain-English
explanation of why the two of them scored the way they did. The explanation is written by
Google Gemini from the match's real score breakdown. It never changes a score or a ranking, never
sees personal data, and falls back to the existing deterministic explanation whenever Gemini is
unavailable.

Success means every "Done when" item in #15 can be ticked:

- Real Gemini examples agree with strong, partial and weak match evidence, and inspecting the
  outbound request shows only the approved fields leave the system.
- Failure, timeout and stale-cache tests pass without blocking search or leaking data;
  authorisation and safe rendering are checked.
- The integrated UI works and the demo evidence is ready for #44.

## Non-goals

- Changing how matches are scored, ranked or filtered. That stays Team A's.
- Explaining matches automatically on search. Explanations are only fetched on click.
- The AI study roadmap generator, icebreakers or chat. Deferred; pitched separately.
- Any provider other than Gemini, though the provider interface allows one later.

## Decisions

| Decision | Choice | Reason |
| --- | --- | --- |
| Provider | Google Gemini API, free tier | Free for coursework; Charlize sets up the key |
| When to generate | Only when the student clicks **Explain this match** | Free-tier rate limits, search stays fast, less data sent |
| Cache | Database table keyed by a hash of the evidence | Survives restarts; invalidates itself when evidence changes |
| Fallback | The existing `MatchScore.explanation()` text | Already deterministic and already shown on every card |
| HTTP client | Spring's built-in `RestClient` | No new library; nothing to add to `pom.xml` |
| Team A code | One new public method, written or approved by Team A | Scores must be computed exactly as search computes them |

## How it works

```
Student clicks "Explain this match" on a MatchCard
  -> GET /api/matches/{candidateId}/explanation?<same search params as the card>
  -> MatchExplanationController          validates, calls one service method
  -> MatchExplanationService
       1. MatchService.evaluate(...)      Team A: the same score the card shows
       2. ExplanationEvidenceFactory      MatchScore -> ExplanationEvidence (allowlist only)
       3. ExplanationCache.find(hash)     hit -> return cached text
       4. ExplanationQuota.tryAcquire()   over the daily limit -> fallback
       5. GeminiExplanationProvider       prompt -> Gemini -> raw text
       6. ExplanationOutputValidator      reject -> fallback
       7. ExplanationCache.save(hash)
  -> MatchExplanationDto { text, source: AI | STANDARD }
```

Any failure in steps 4 to 6 returns the deterministic explanation with `source: STANDARD`. The
endpoint never fails because Gemini failed. It fails only for the reasons search fails: not signed
in, not a student, an invalid search, or a candidate who isn't an eligible match.

## Backend components

New package `com.studybuddy.matchexplanation`. Controllers, services and DTOs follow the existing
layering: the controller touches no repository, and no entity leaves the service.

| Class | Responsibility |
| --- | --- |
| `MatchExplanationController` | `GET /api/matches/{candidateId}/explanation`. Binds the search parameters to `MatchSearchCriteria`, calls the service, returns `MatchExplanationDto`. |
| `MatchExplanationService` | Runs the flow above. Not transactional: `MatchService.evaluate` and the cache each use their own short transaction, so no database connection is held during the Gemini call. The only class that knows the order of the steps. |
| `ExplanationEvidence` (record) | The allowlist: the only data that may be sent to Gemini. See [Evidence allowlist](#evidence-allowlist). |
| `ExplanationEvidenceFactory` | Builds `ExplanationEvidence` from Team A's evaluation result, rounding numbers so near-identical matches share a cache entry. |
| `ExplanationPromptBuilder` | Loads the versioned template from `resources/prompts/match-explanation-v1.txt` and inserts the evidence as JSON. Exposes `PROMPT_VERSION`. |
| `ExplanationProvider` (interface) | `Optional<String> explain(String prompt)`. Empty means "no usable answer". |
| `GeminiExplanationProvider` | Calls Gemini's `generateContent` endpoint through `RestClient` with the configured model, timeout and token limit. Returns empty on any error or timeout and logs the reason, without the prompt. |
| `ExplanationOutputValidator` | Accepts the text only if it is non-blank and within the length limit, and contains no banned claims ("probability", "chance of success", "guarantee", "likely to succeed", or any `%` sign). |
| `ExplanationCache` | Finds and saves rows in `match_explanations` by evidence hash. A duplicate insert from two simultaneous clicks is ignored, not an error. |
| `ExplanationHasher` | SHA-256 of the canonical evidence JSON + prompt version + model ID. |
| `ExplanationQuota` | In-memory daily counter of Gemini calls, reset at midnight Singapore time. Over the limit means fallback. A restart resets it; that is acceptable for coursework and is documented. |
| `MatchExplanation` (entity), `MatchExplanationRepository` | The cache row. Never returned to the API. |
| `MatchExplanationDto` (record) | `candidateId`, `text`, `source` (`AI` or `STANDARD`). |
| `ExplanationSource` (enum) | `AI`, `STANDARD`. |
| `AiExplanationProperties` (record) | Typed binding of `app.ai.explanation.*`. Startup fails on invalid values, the same as `MatchingProperties`. |

Choosing between Gemini and the fallback is done by the service checking an `Optional`, not by a
`switch` on provider type. A second provider later would be another `ExplanationProvider`.

### Evidence allowlist

`ExplanationEvidence` holds only these fields. Nothing else can reach the prompt, because the
prompt builder accepts only this type.

| Field | Example | Rounding |
| --- | --- | --- |
| `quality` | `GOOD` | none |
| `totalScore` | `0.72` | 2 decimal places |
| `strategy` | `BALANCED` | none |
| `criteria[]` | `{criterion: AVAILABILITY, score: 0.40, enabled: true}` | score to 2 dp |
| `sharedHoursPerWeek` | `3.5` | nearest 0.5 |
| `course` | `{code: "IS442", name: "Object Oriented Programming"}` or null | none |
| `sharedGoals` | `[EXAM_PREPARATION]` | sorted |
| `studyModes` | `{searcher: ONLINE, candidate: EITHER}` | none |
| `groupSizes` | `{searcher: ONE_TO_ONE, candidate: SMALL_GROUP}` | none |

**Never included:** either student's name, ID, email, contact number, school, programme, year,
messages, availability slots, connections or group memberships. A unit test
serialises an `ExplanationEvidence` built from a fully populated student and asserts none of those
values appear in the JSON.

## Team A dependency

The score for one candidate is currently computed inside `MatchService.search`, using private
helpers. E1 needs the same calculation for a single candidate, so the explanation always matches
the card the student clicked.

**Request to Team A:** add a public method to `MatchService` (or a small class it delegates to),
for example:

```java
public MatchEvaluation evaluate(MatchSearchCriteria search, Long actorId, Long candidateId)
```

returning `MatchEvaluation(strategy, result, searcherStudyMode, searcherStudyGoals, searcherGroupSize,
candidateGroupSize)`, where `result` is the same `MatchResultDto` search would return for that
candidate. It holds no entities. It should apply the
same validation, eligibility check (`access.eligibleStudentIds()`, not self) and settings snapshot
as `search`, and throw the same exceptions. It reuses the same private helpers as `search`; the implementation plan leaves `search` itself unchanged
to keep the Team A diff small, and a test asserts both return the same result.

This is a service-to-service call returning a value object, not an entity, so both team rules still
hold. If Team A prefers, Charlize writes it and Team A reviews it. **Nothing is committed to
`matching/` until they agree.**

Also needed from Team A: approval for one button in `MatchCard.tsx` (plus a `search` prop) and for
passing `search` to it from `FindBuddiesPanel.tsx`.

## Database

Migration `backend/src/main/resources/db/migrations/<timestamp>_match_explanation_cache.sql`,
repeat-safe, with RLS enabled and browser-role grants revoked, following
`20261006170448_backend_only_database_access.sql`. The migration-count assertions in the database
boundary tests are updated to include the new table.

```
match_explanations
  id               bigint generated by default as identity primary key
  evidence_hash    varchar(64) not null unique  -- SHA-256 hex of evidence + prompt version + model
  prompt_version   varchar(16) not null
  model            varchar(64) not null
  explanation_text text not null
  created_at       timestamptz not null default now()
```

The cache holds no student IDs, so deleting or deactivating an account needs no cleanup here.
Changed preferences, admin weight changes, a new prompt version or a new model all produce a new
hash, so stale rows are simply never read again. No explicit invalidation code is needed.

## API

Recorded in `docs/API_CONTRACT.md` before any client code is written.

`GET /api/matches/{candidateId}/explanation`

- Query parameters: identical to `GET /api/matches` (`courseId`, `studyGoal`, `studyMode`,
  `groupSize`, `minSharedHours`, `minQuality`). The frontend sends the parameters of the search
  that produced the card.
- `200`: `{ "candidateId": 42, "text": "...", "source": "AI" }`
- `400`: invalid search, the same as search.
- `401`: not signed in. `403`: not a student.
- `404`: candidate missing, inactive, not eligible, or the requester themself.

## Configuration

Added to `application.yml` and to the configuration table in `README.md`, in the same change.

| Key | Default | Meaning |
| --- | --- | --- |
| `app.ai.explanation.enabled` | `false` | Off unless a key is configured; off means always use the fallback |
| `app.ai.explanation.base-url` | `https://generativelanguage.googleapis.com/v1beta` | Gemini API base URL |
| `app.ai.explanation.model` | set after checking the current free-tier model list | Gemini model ID |
| `app.ai.explanation.api-key` | none in `application.yml` | Set only in `application-local.yml` |
| `app.ai.explanation.timeout` | `5s` | Fall back after this |
| `app.ai.explanation.max-output-tokens` | `200` | Keeps replies short |
| `app.ai.explanation.max-characters` | `600` | Validator's length limit |
| `app.ai.explanation.daily-call-limit` | below the free tier's daily cap | Fall back once reached |
| `app.ai.explanation.quota-zone` | `Asia/Singapore` | Midnight in this zone resets the daily limit |

The key is never logged, never sent to the frontend, and never committed.

## Prompt

Stored at `backend/src/main/resources/prompts/match-explanation-v1.txt` and versioned by file name.
It instructs the model to:

- write two or three sentences, addressed to the student as "you" and "this student";
- explain only from the evidence provided: strongest criteria first, then the weakest;
- mention shared hours and the course when they are present;
- never invent facts, names or preferences, and never describe the score as a probability or
  promise of success;
- return JSON `{"explanation": "..."}`. The request sets `responseMimeType: application/json`.

The full template and three real input/output examples (strong, partial, weak) are recorded in
`docs/AI_USAGE.md` for #43.

## Frontend

New folder `frontend/src/features/matchexplanation/`:

- `api.ts`: `fetchMatchExplanation(candidateId, searchParams)` through the shared API client.
- `MatchExplanationPanel.tsx`: owns the button and its states.
  - Idle: **Explain this match** button.
  - Loading: "Writing an explanation…" with the button disabled.
  - `AI`: the text, labelled "AI-generated by Gemini. Scores are calculated separately and are not
    changed by this text."
  - `STANDARD`: no repeated text, since the card already shows it; instead "An AI explanation isn't
    available right now. The summary above is the standard explanation." No error tone, because this
    is normal behaviour.
  - Error (network or 4xx): "Couldn't load an explanation." with **Try again**.
  - Text is rendered as a plain React text node, never as HTML.
- `MatchCard.tsx` (Team A): one line rendering `<MatchExplanationPanel>` under the existing
  explanation. The existing breakdown stays visible.

## Error handling

| Situation | Result |
| --- | --- |
| Feature disabled or no API key | `STANDARD` text, no Gemini call |
| Daily limit reached | `STANDARD` |
| Gemini timeout, 4xx, 5xx or network error | `STANDARD`; reason logged without prompt or key |
| Gemini reply not JSON, blank, too long or with a banned claim | `STANDARD` |
| Two clicks race on the same evidence | Both get an answer; one cache row is kept |
| Candidate deactivated between search and click | `404`; panel shows the error state |

## Testing

Backend unit tests:

- `ExplanationEvidenceFactoryTest`: rounding; a fully populated pair produces JSON containing no
  names, IDs, email, contact number, school, programme or availability.
- `ExplanationHasherTest`: same evidence gives the same hash; any change in evidence, prompt version
  or model changes it.
- `ExplanationOutputValidatorTest`: accepts a good answer; rejects blank, over-length, non-JSON,
  "probability" and invented percentages.
- `ExplanationPromptBuilderTest`: the prompt contains the evidence JSON and nothing else from the
  student.
- `MatchExplanationServiceTest`, with a fake provider: cache hit skips the provider; provider empty
  gives `STANDARD`; validator rejection gives `STANDARD`; quota exhausted gives `STANDARD`; a good
  answer is cached.
- `GeminiExplanationProviderTest` against a local stub server: request body contains only the
  prompt and generation settings; a timeout returns empty.

Backend HTTP test, `MatchExplanationHttpTest`: `401` signed out, `403` admin, `404` self, `404`
inactive candidate, `200` with `STANDARD` when the feature is disabled, which is the test-profile
default.

Frontend tests: idle, loading, `AI` label, `STANDARD` label, error with retry, and that text
containing `<b>` renders literally.

Manual, for #15 and #44: with a real key, record a strong, a partial and a weak example and check
each against its breakdown. Then capture one outbound request, through the debug log or the stub,
showing only allowlisted fields.

## Documentation updated in the same pull request

- `docs/API_CONTRACT.md`: the endpoint.
- `README.md`: configuration rows, and Gemini added to the libraries and services table with its
  terms and free-tier data-use note.
- `docs/AI_USAGE.md`: prompt, input fields, examples, limitations.
- `docs/REQUIREMENT_COVERAGE.md`: an E1 section mapping each #15 requirement to class and test.
- `docs/DESIGN_DECISIONS.md`: on-click generation, evidence hashing, fallback policy.

## Open items before implementation

1. Team A agrees to `MatchService.evaluate` and the `MatchCard` button, and decides who writes it.
2. Charlize confirms the Gemini model ID and the free tier's current daily and per-minute limits,
   then sets `daily-call-limit` below them.
3. The team accepts Gemini free-tier data-use terms for anonymous evidence; this is recorded in
   `AI_USAGE.md`.
