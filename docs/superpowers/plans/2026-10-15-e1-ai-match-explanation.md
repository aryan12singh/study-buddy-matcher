# E1 AI Match Explanation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A student can click **Explain this match** on a match card and read a short Gemini-written explanation of that match, built only from anonymous score evidence, with the existing deterministic explanation as a fallback.

**Architecture:** A new `matchexplanation` package exposes `GET /api/matches/{candidateId}/explanation`. It asks Team A's `MatchService.evaluate` for the same score the card shows, reduces it to an allowlisted `ExplanationEvidence` record, and looks up a database cache keyed by a SHA-256 hash of that evidence. On a miss, within a daily quota, it calls Gemini through Spring's `RestClient`, validates the reply and caches it. Any failure returns the standard text with `source: STANDARD`. A small React panel on `MatchCard` fetches it on click.

**Tech Stack:** Java 21, Spring Boot 4 (WebMVC, Data JPA, Validation), Jackson 3 (`tools.jackson`), PostgreSQL migrations, JUnit 6 + Mockito, JDK `HttpServer` for the stub; React 19 + TypeScript, Axios shared client, Vitest + Testing Library.

**Spec:** `docs/superpowers/specs/2026-10-14-e1-ai-match-explanation-design.md`

## Before you start (not code)

1. **Team A has agreed** to Task 1 (`MatchService.evaluate` and `MatchEvaluation`) and to Task 9's two small edits (`MatchCard.tsx` and `FindBuddiesPanel.tsx`), and has said who writes Task 1. Do not start Task 1 or Task 9 without that.
2. **Gemini key:** create a free API key in Google AI Studio. Check the current free-tier model list and its requests-per-day and per-minute limits. If `gemini-2.5-flash` (the default in Task 4) is retired, use the current Flash model ID instead.
3. **Local secret file:** add the following to `backend/src/main/resources/application-local.yml`, which is git-ignored. Never put the key anywhere else.

   ```yaml
   app:
     ai:
       explanation:
         enabled: true
         api-key: <your Gemini key>
   ```

## Global Constraints

- Controllers never touch repositories; services never return entities to the API (AGENTS.md "The two rules").
- No new library: nothing is added to `pom.xml` or `package.json`. HTTP goes through Spring's `RestClient`; the test stub uses the JDK's `com.sun.net.httpserver.HttpServer`.
- The API key lives only in `application-local.yml`. It is never logged, never returned to the frontend, never in a tracked file.
- Only `ExplanationEvidence` fields reach Gemini. Never names, IDs, emails, contact numbers, school, programme, year, messages, availability slots, connections or group memberships.
- Explanations are fetched only on click, never during search.
- Every new config key gets a row in the README configuration table in the same commit.
- Constructor injection only; value objects are immutable records; no magic numbers (tunables come from `app.ai.explanation.*`).
- Commits: present tense, lower case after the colon, no full stop, **no AI attribution lines**.
- Branch: `feat/team-c/e1-ai-match-explanation` (local; do not push until the user says so).
- Backend tests run through `scripts/test-backend.sh`, which needs Docker. Frontend: `cd frontend && npm test && npm run lint && npm run build`.

## Review Focus

1. **The explanation contradicts the card.** The search params sent for the explanation must be the ones that produced the card, or the scores differ. Pinned by Task 1's `evaluateScoresExactlyAsSearchDoes` and Task 9's params test.
2. **Gemini answers but says something misleading** ("80% chance you'll succeed"). Pinned by Task 3's validator tests for `%`, "probability", "chance of success" and "guarantee".
3. **Gemini is slow on SMU wifi during the demo.** The student must get the standard text within the timeout, not a spinner forever. Pinned by Task 5's `slowReplyReturnsEmpty`.
4. **Two students' identical evidence shares one cached answer, and a duplicate insert never errors.** Pinned by Task 6's `savingTheSameEvidenceTwiceKeepsOneRow`.
5. **The AI text contains markup** (`<b>`, `<script>`). It must show literally, not render. Pinned by Task 9's `rendersTextLiterally`.

---

## File map

**Backend: create** (all under `backend/src/main/java/com/studybuddy/`)

| File | Responsibility |
| --- | --- |
| `matching/MatchEvaluation.java` | (Team A) One candidate's search-identical result plus the searcher's preferences; no entities |
| `matchexplanation/CriterionEvidence.java` | One criterion's rounded score and enabled flag |
| `matchexplanation/CourseEvidence.java` | Course code and name |
| `matchexplanation/ExplanationEvidence.java` | The allowlist sent to Gemini |
| `matchexplanation/ExplanationEvidenceFactory.java` | `MatchEvaluation` → `ExplanationEvidence`, with rounding |
| `matchexplanation/ExplanationHasher.java` | SHA-256 of evidence + prompt version + model |
| `matchexplanation/ExplanationPromptBuilder.java` | Loads the versioned template and inserts evidence JSON |
| `matchexplanation/ExplanationOutputValidator.java` | Accepts or rejects Gemini's raw reply |
| `matchexplanation/AiExplanationProperties.java` | Typed `app.ai.explanation.*` |
| `matchexplanation/AiExplanationConfiguration.java` | Enables the properties; provides the quota clock |
| `matchexplanation/ExplanationQuota.java` | Daily Gemini call counter |
| `matchexplanation/ExplanationProvider.java` | Interface: prompt → optional text |
| `matchexplanation/GeminiExplanationProvider.java` | Gemini `generateContent` over `RestClient` |
| `matchexplanation/MatchExplanation.java` | Cache entity |
| `matchexplanation/MatchExplanationRepository.java` | Find by hash; insert-if-absent |
| `matchexplanation/ExplanationCache.java` | Cache reads and writes in plain strings |
| `matchexplanation/ExplanationSource.java` | `AI`, `STANDARD` |
| `matchexplanation/MatchExplanationDto.java` | API response |
| `matchexplanation/MatchExplanationAssembler.java` | Builds the DTO |
| `matchexplanation/MatchExplanationService.java` | Orchestrates the flow |
| `matchexplanation/MatchExplanationController.java` | The endpoint |

**Backend: create (resources and tests)**

- `backend/src/main/resources/prompts/match-explanation-v1.txt`
- `backend/src/main/resources/db/migrations/20261016090000_match_explanation_cache.sql`
- `backend/src/test/java/com/studybuddy/matchexplanation/ExplanationEvidenceFactoryTest.java`
- `backend/src/test/java/com/studybuddy/matchexplanation/ExplanationHasherTest.java`
- `backend/src/test/java/com/studybuddy/matchexplanation/ExplanationPromptBuilderTest.java`
- `backend/src/test/java/com/studybuddy/matchexplanation/ExplanationOutputValidatorTest.java`
- `backend/src/test/java/com/studybuddy/matchexplanation/ExplanationQuotaTest.java`
- `backend/src/test/java/com/studybuddy/matchexplanation/GeminiExplanationProviderTest.java`
- `backend/src/test/java/com/studybuddy/matchexplanation/MatchExplanationServiceTest.java`
- `backend/src/test/java/com/studybuddy/matchexplanation/ExplanationFixtures.java`
- `backend/src/test/java/com/studybuddy/integration/MatchExplanationHttpTest.java`

**Backend: modify**

- `backend/src/main/java/com/studybuddy/matching/MatchService.java` (Team A): add `evaluate`
- `backend/src/test/java/com/studybuddy/matching/MatchServiceTest.java` (Team A): tests for `evaluate`
- `backend/src/main/resources/application.yml`: `app.ai.explanation` block
- `backend/src/test/java/com/studybuddy/integration/PostgresHttpTest.java`: truncate the new table
- `backend/src/test/java/com/studybuddy/integration/DatabaseBoundaryTest.java`: browser roles denied on the new table

**Frontend: create**

- `frontend/src/features/matchexplanation/api.ts`
- `frontend/src/features/matchexplanation/MatchExplanationPanel.tsx`
- `frontend/src/features/matchexplanation/MatchExplanationPanel.test.tsx`

**Frontend: modify (Team A files, flagged in the PR)**

- `frontend/src/features/matching/MatchCard.tsx`: new `search` prop, render the panel
- `frontend/src/features/matching/FindBuddiesPanel.tsx:143`: pass `search`

**Docs: modify:** `README.md`, `docs/API_CONTRACT.md`, `docs/AI_USAGE.md`, `docs/REQUIREMENT_COVERAGE.md`, `docs/DESIGN_DECISIONS.md`.

---

### Task 1: Team A — evaluate one candidate exactly as search does

**Files:**
- Create: `backend/src/main/java/com/studybuddy/matching/MatchEvaluation.java`
- Modify: `backend/src/main/java/com/studybuddy/matching/MatchService.java` (add a method after `search`)
- Test: `backend/src/test/java/com/studybuddy/matching/MatchServiceTest.java`

**Interfaces:**
- Consumes: existing `MatchService` fields (`access`, `config`, `properties`, `scorer`, `assembler`, `relationships`) and private helpers `validate`, `availabilityByStudent`, `profileOf`.
- Produces: `public MatchEvaluation evaluate(MatchSearchCriteria search, Long actorId, Long candidateId)`, and `public record MatchEvaluation(MatchingStrategyType strategy, MatchResultDto result, StudyMode searcherStudyMode, Set<StudyGoal> searcherStudyGoals, GroupSizePreference searcherGroupSize, GroupSizePreference candidateGroupSize)`.

- [ ] **Step 1: Write the failing tests** — append to `MatchServiceTest` (it already has `me`, `strong`, `weak`, `courseSearch(...)` and mocks from `setUp`). Add the imports `com.studybuddy.student.StudentNotFoundException` and `com.studybuddy.student.GroupSizePreference`.

```java
    @Test
    void evaluateScoresExactlyAsSearchDoes() {
        when(access.eligibleStudent(2L)).thenReturn(strong);
        MatchResultDto fromSearch = service.search(courseSearch(null), ME).matches().stream()
                .filter(result -> result.studentId() == 2L).findFirst().orElseThrow();

        MatchEvaluation evaluation = service.evaluate(courseSearch(null), ME, 2L);

        assertEquals(fromSearch, evaluation.result());
        assertEquals(MatchingStrategyType.BALANCED, evaluation.strategy());
        assertEquals(StudyMode.ONLINE, evaluation.searcherStudyMode());
        assertEquals(java.util.Set.of(StudyGoal.EXAM_PREPARATION), evaluation.searcherStudyGoals());
        assertEquals(GroupSizePreference.EITHER, evaluation.candidateGroupSize());
    }

    @Test
    void evaluateRejectsTheSearcherThemself() {
        assertThrows(StudentNotFoundException.class, () -> service.evaluate(courseSearch(null), ME, ME));
    }

    @Test
    void evaluateRejectsAnInvalidSearchBeforeLookingUpTheCandidate() {
        assertThrows(InvalidMatchSearchException.class,
                () -> service.evaluate(new MatchSearchCriteria(null, null, null, null, null, null), ME, 2L));
    }

    @Test
    void evaluatePassesOnIneligibleCandidateErrors() {
        when(access.eligibleStudent(9L)).thenThrow(new StudentNotFoundException(9L));
        assertThrows(StudentNotFoundException.class, () -> service.evaluate(courseSearch(null), ME, 9L));
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `cd backend && ./mvnw -q test -Dtest=MatchServiceTest`
Expected: compilation FAIL, `cannot find symbol: method evaluate` and `class MatchEvaluation`. These unit tests use Mockito only; no Docker needed.

- [ ] **Step 3: Create `MatchEvaluation`**

```java
package com.studybuddy.matching;

import com.studybuddy.matching.strategy.MatchingStrategyType;
import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;

import java.util.Set;

/**
 * One candidate scored exactly as search scores them, plus the searcher's own preferences,
 * for features that explain a match. Holds no entities, so nothing private can leak from it.
 */
public record MatchEvaluation(MatchingStrategyType strategy, MatchResultDto result, StudyMode searcherStudyMode,
        Set<StudyGoal> searcherStudyGoals, GroupSizePreference searcherGroupSize,
        GroupSizePreference candidateGroupSize) {

    public MatchEvaluation {
        searcherStudyGoals = Set.copyOf(searcherStudyGoals);
    }
}
```

- [ ] **Step 4: Add `evaluate` to `MatchService`**, directly after `search(...)`. Add the imports `com.studybuddy.student.GroupSizePreference` and `com.studybuddy.student.StudentNotFoundException`.

```java
    /**
     * Scores one candidate with the same validation, settings snapshot and scorer as {@link #search},
     * so an explanation always describes the score on the card the student clicked.
     */
    public MatchEvaluation evaluate(MatchSearchCriteria search, Long actorId, Long candidateId) {
        Student self = access.requireStudent(actorId);
        validate(search);
        if (self.getId().equals(candidateId)) {
            throw new StudentNotFoundException(candidateId);
        }
        Student other = access.eligibleStudent(candidateId);
        Map<Long, WeeklyAvailability> availability = availabilityByStudent();
        MatchingProfile searcher = profileOf(self, availability);
        MatchingProfile candidate = profileOf(other, availability);
        MatchingConfigSnapshot settings = config.snapshot();
        MatchScore score = scorer.score(searcher, candidate, search, settings.weights(), settings.enabledCriteria(),
                properties.quality());
        RelationshipState relationship = relationships.of(self.getId())
                .getOrDefault(candidateId, RelationshipState.STRANGER);
        MatchResultDto result = assembler.toDto(searcher, candidate, score, search, settings.enabledCriteria(), relationship);
        return new MatchEvaluation(settings.strategy().type(), result, self.getPreferredStudyMode(), self.getStudyGoals(),
                GroupSizePreference.fromRange(self.getPreferredGroupSizeMin(), self.getPreferredGroupSizeMax()),
                GroupSizePreference.fromRange(other.getPreferredGroupSizeMin(), other.getPreferredGroupSizeMax()));
    }
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `cd backend && ./mvnw -q test -Dtest=MatchServiceTest`
Expected: PASS, all existing and four new tests.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/studybuddy/matching/MatchEvaluation.java backend/src/main/java/com/studybuddy/matching/MatchService.java backend/src/test/java/com/studybuddy/matching/MatchServiceTest.java
git commit -m "feat: evaluate one match candidate exactly as search scores them"
```

---

### Task 2: Evidence allowlist, rounding and hashing

**Files:**
- Create: `matchexplanation/CriterionEvidence.java`, `CourseEvidence.java`, `ExplanationEvidence.java`, `ExplanationEvidenceFactory.java`, `ExplanationHasher.java`
- Test: `backend/src/test/java/com/studybuddy/matchexplanation/ExplanationFixtures.java`, `ExplanationEvidenceFactoryTest.java`, `ExplanationHasherTest.java`

**Interfaces:**
- Consumes: `MatchEvaluation` (Task 1), `MatchResultDto`, `CriterionScoreDto`, `RelevantCourseDto`.
- Produces:
  - `record ExplanationEvidence(MatchQuality quality, double totalScore, MatchingStrategyType strategy, List<CriterionEvidence> criteria, double sharedHoursPerWeek, CourseEvidence course, List<StudyGoal> sharedGoals, StudyMode searcherStudyMode, StudyMode candidateStudyMode, GroupSizePreference searcherGroupSize, GroupSizePreference candidateGroupSize)`
  - `ExplanationEvidenceFactory#from(MatchEvaluation) : ExplanationEvidence`
  - `ExplanationHasher#hash(ExplanationEvidence, String promptVersion, String model) : String` (64 lowercase hex characters)
  - `ExplanationFixtures.evaluation()` and `ExplanationFixtures.evaluation(double totalScore)` for later tests.

- [ ] **Step 1: Create the shared test fixture** `ExplanationFixtures.java`

```java
package com.studybuddy.matchexplanation;

import com.studybuddy.matching.CriterionScoreDto;
import com.studybuddy.matching.MatchEvaluation;
import com.studybuddy.matching.MatchQuality;
import com.studybuddy.matching.MatchResultDto;
import com.studybuddy.matching.RelevantCourseDto;
import com.studybuddy.matching.SharedSlotDto;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import com.studybuddy.profile.RelationshipState;
import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import tools.jackson.databind.json.JsonMapper;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/** A fully populated match evaluation, including every private field the evidence must drop. */
final class ExplanationFixtures {
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final long CANDIDATE_ID = 424242L;

    private ExplanationFixtures() {
    }

    static MatchEvaluation evaluation() {
        return evaluation(0.7234);
    }

    static MatchEvaluation evaluation(double totalScore) {
        MatchResultDto result = new MatchResultDto(CANDIDATE_ID, "Jamie Lee", "SCIS", "Information Systems", 2,
                StudyMode.EITHER, List.of(StudyGoal.EXAM_PREPARATION, StudyGoal.PROBLEM_SOLVING), MatchQuality.GOOD,
                totalScore, "good match: same course", new RelevantCourseDto(4L, "IS442", "Object Oriented Programming"),
                List.of(new CriterionScoreDto(MatchingCriterion.COURSE, 1.0, true),
                        new CriterionScoreDto(MatchingCriterion.AVAILABILITY, 0.3333, true),
                        new CriterionScoreDto(MatchingCriterion.STUDY_MODE, 1.0, true),
                        new CriterionScoreDto(MatchingCriterion.STUDY_GOAL, 0.5, true),
                        new CriterionScoreDto(MatchingCriterion.GROUP_SIZE, 0.5, false)),
                3.4, List.of(new SharedSlotDto(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0))),
                RelationshipState.STRANGER);
        return new MatchEvaluation(MatchingStrategyType.BALANCED, result, StudyMode.ONLINE,
                Set.of(StudyGoal.EXAM_PREPARATION, StudyGoal.CONCEPT_REVIEW), GroupSizePreference.ONE_TO_ONE,
                GroupSizePreference.EITHER);
    }
}
```

Check `SharedSlotDto`'s constructor types first with `grep -n record backend/src/main/java/com/studybuddy/matching/SharedSlotDto.java`. If its fields are not `(DayOfWeek, LocalTime, LocalTime)`, match them.

- [ ] **Step 2: Write the failing factory and hasher tests**

`ExplanationEvidenceFactoryTest.java`:

```java
package com.studybuddy.matchexplanation;

import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.student.StudyGoal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ExplanationEvidenceFactoryTest {
    private final ExplanationEvidenceFactory factory = new ExplanationEvidenceFactory();

    @Test
    void roundsScoresToHundredthsAndHoursToHalfHours() {
        ExplanationEvidence evidence = factory.from(ExplanationFixtures.evaluation());

        assertEquals(0.72, evidence.totalScore());
        assertEquals(3.5, evidence.sharedHoursPerWeek());
        assertEquals(0.33, evidence.criteria().stream()
                .filter(criterion -> criterion.criterion() == MatchingCriterion.AVAILABILITY)
                .findFirst().orElseThrow().score());
    }

    @Test
    void keepsOnlyGoalsBothStudentsShareInAStableOrder() {
        assertEquals(List.of(StudyGoal.EXAM_PREPARATION), factory.from(ExplanationFixtures.evaluation()).sharedGoals());
    }

    @Test
    void keepsTheDisabledFlagSoTheModelKnowsWhatDidNotCount() {
        assertFalse(factory.from(ExplanationFixtures.evaluation()).criteria().getLast().enabled());
    }

    @Test
    void neverCarriesPersonalDetails() {
        String json = ExplanationFixtures.JSON.writeValueAsString(factory.from(ExplanationFixtures.evaluation()));

        for (String forbidden : new String[] {"Jamie", "SCIS", "Information Systems", "424242", "studentId",
                "yearOfStudy", "school", "programme", "MONDAY", "09:00", "relationship", "good match: same course"}) {
            assertFalse(json.contains(forbidden), forbidden + " leaked into " + json);
        }
    }

    @Test
    void anUnknownCourseStaysEmpty() {
        var evaluation = ExplanationFixtures.evaluation();
        var result = evaluation.result();
        var withoutCourse = new com.studybuddy.matching.MatchEvaluation(evaluation.strategy(),
                new com.studybuddy.matching.MatchResultDto(result.studentId(), result.name(), result.school(),
                        result.programme(), result.yearOfStudy(), result.studyMode(), result.studyGoals(), result.quality(),
                        result.totalScore(), result.explanation(), null, result.breakdown(), result.sharedHoursPerWeek(),
                        result.sharedSlots(), result.relationship()),
                evaluation.searcherStudyMode(), evaluation.searcherStudyGoals(), evaluation.searcherGroupSize(),
                evaluation.candidateGroupSize());

        assertEquals(null, factory.from(withoutCourse).course());
    }
}
```

`ExplanationHasherTest.java`:

```java
package com.studybuddy.matchexplanation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplanationHasherTest {
    private final ExplanationHasher hasher = new ExplanationHasher(ExplanationFixtures.JSON);
    private final ExplanationEvidenceFactory factory = new ExplanationEvidenceFactory();

    @Test
    void sameEvidenceVersionAndModelGiveTheSameHash() {
        String first = hasher.hash(factory.from(ExplanationFixtures.evaluation()), "v1", "model-a");
        String second = hasher.hash(factory.from(ExplanationFixtures.evaluation()), "v1", "model-a");

        assertEquals(first, second);
        assertTrue(first.matches("[0-9a-f]{64}"), first);
    }

    @Test
    void scoresThatRoundTheSameShareAHash() {
        assertEquals(hasher.hash(factory.from(ExplanationFixtures.evaluation(0.7234)), "v1", "m"),
                hasher.hash(factory.from(ExplanationFixtures.evaluation(0.7189)), "v1", "m"));
    }

    @Test
    void anyChangeInEvidencePromptOrModelChangesTheHash() {
        ExplanationEvidence evidence = factory.from(ExplanationFixtures.evaluation());
        String base = hasher.hash(evidence, "v1", "model-a");

        assertNotEquals(base, hasher.hash(factory.from(ExplanationFixtures.evaluation(0.9)), "v1", "model-a"));
        assertNotEquals(base, hasher.hash(evidence, "v2", "model-a"));
        assertNotEquals(base, hasher.hash(evidence, "v1", "model-b"));
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `cd backend && ./mvnw -q test -Dtest='Explanation*Test'`
Expected: compilation FAIL, `ExplanationEvidenceFactory` not found.

- [ ] **Step 4: Create the evidence records**

`CriterionEvidence.java`:

```java
package com.studybuddy.matchexplanation;

import com.studybuddy.matching.scoring.MatchingCriterion;

/** One criterion's rounded score, and whether the admin's settings let it count. */
public record CriterionEvidence(MatchingCriterion criterion, double score, boolean enabled) {
}
```

`CourseEvidence.java`:

```java
package com.studybuddy.matchexplanation;

/** The course the match was about. A course code is public catalogue data, not personal data. */
public record CourseEvidence(String code, String name) {
}
```

`ExplanationEvidence.java`:

```java
package com.studybuddy.matchexplanation;

import com.studybuddy.matching.MatchQuality;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;

import java.util.List;

/**
 * The only data that may be sent to the AI provider: anonymous compatibility evidence.
 * Adding a field here widens what leaves the system, so it needs the same review as a new
 * API field. Names, IDs, contact details, school, year and schedules are deliberately absent.
 */
public record ExplanationEvidence(MatchQuality quality, double totalScore, MatchingStrategyType strategy,
        List<CriterionEvidence> criteria, double sharedHoursPerWeek, CourseEvidence course, List<StudyGoal> sharedGoals,
        StudyMode searcherStudyMode, StudyMode candidateStudyMode, GroupSizePreference searcherGroupSize,
        GroupSizePreference candidateGroupSize) {

    public ExplanationEvidence {
        criteria = List.copyOf(criteria);
        sharedGoals = List.copyOf(sharedGoals);
    }
}
```

- [ ] **Step 5: Create `ExplanationEvidenceFactory`**

```java
package com.studybuddy.matchexplanation;

import com.studybuddy.matching.MatchEvaluation;
import com.studybuddy.matching.MatchResultDto;
import com.studybuddy.student.StudyGoal;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reduces a match evaluation to its allowlisted evidence. Rounding means two matches that
 * differ only in the third decimal place share one cached explanation.
 */
@Component
public class ExplanationEvidenceFactory {
    /** Scores are kept to two decimal places. */
    private static final double SCORE_PRECISION = 100.0;
    /** Shared hours are kept to the nearest half hour. */
    private static final double HOURS_PRECISION = 2.0;

    public ExplanationEvidence from(MatchEvaluation evaluation) {
        MatchResultDto result = evaluation.result();
        List<CriterionEvidence> criteria = result.breakdown().stream()
                .map(score -> new CriterionEvidence(score.criterion(), round(score.score(), SCORE_PRECISION), score.enabled()))
                .toList();
        CourseEvidence course = result.relevantCourse() == null ? null
                : new CourseEvidence(result.relevantCourse().code(), result.relevantCourse().name());
        List<StudyGoal> sharedGoals = result.studyGoals().stream()
                .filter(evaluation.searcherStudyGoals()::contains)
                .sorted()
                .toList();
        return new ExplanationEvidence(result.quality(), round(result.totalScore(), SCORE_PRECISION),
                evaluation.strategy(), criteria, round(result.sharedHoursPerWeek(), HOURS_PRECISION), course, sharedGoals,
                evaluation.searcherStudyMode(), result.studyMode(), evaluation.searcherGroupSize(),
                evaluation.candidateGroupSize());
    }

    private static double round(double value, double precision) {
        return Math.round(value * precision) / precision;
    }
}
```

- [ ] **Step 6: Create `ExplanationHasher`**

```java
package com.studybuddy.matchexplanation;

import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * The cache key for an explanation. Any change to the evidence, the prompt version or the model
 * produces a different key, so a stale explanation is never read again and needs no deletion.
 */
@Component
public class ExplanationHasher {
    private final JsonMapper json;

    public ExplanationHasher(JsonMapper json) {
        this.json = json;
    }

    public String hash(ExplanationEvidence evidence, String promptVersion, String model) {
        String canonical = json.writeValueAsString(evidence) + "\n" + promptVersion + "\n" + model;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Every Java runtime provides SHA-256", impossible);
        }
    }
}
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `cd backend && ./mvnw -q test -Dtest='Explanation*Test'`
Expected: PASS (8 tests).

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/studybuddy/matchexplanation backend/src/test/java/com/studybuddy/matchexplanation
git commit -m "feat: reduce a match to anonymous explanation evidence and hash it"
```

---

### Task 3: Prompt template, prompt builder and output validator

**Files:**
- Create: `backend/src/main/resources/prompts/match-explanation-v1.txt`, `matchexplanation/ExplanationPromptBuilder.java`, `matchexplanation/ExplanationOutputValidator.java`
- Test: `ExplanationPromptBuilderTest.java`, `ExplanationOutputValidatorTest.java`

**Interfaces:**
- Consumes: `ExplanationEvidence`, `ExplanationFixtures` (Task 2).
- Produces:
  - `ExplanationPromptBuilder.PROMPT_VERSION` (`"v1"`)
  - `ExplanationPromptBuilder#build(ExplanationEvidence) : String`
  - `ExplanationOutputValidator(int maxCharacters, JsonMapper json)`; in Task 4 Spring builds it from properties.
  - `ExplanationOutputValidator#validate(String raw) : Optional<String>`

- [ ] **Step 1: Write the failing tests**

`ExplanationPromptBuilderTest.java`:

```java
package com.studybuddy.matchexplanation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplanationPromptBuilderTest {
    private final ExplanationPromptBuilder builder = new ExplanationPromptBuilder(ExplanationFixtures.JSON);

    @Test
    void insertsTheEvidenceJsonAndNothingPersonal() {
        String prompt = builder.build(new ExplanationEvidenceFactory().from(ExplanationFixtures.evaluation()));

        assertTrue(prompt.contains("\"totalScore\":0.72"), prompt);
        assertTrue(prompt.contains("IS442"), prompt);
        assertFalse(prompt.contains("{{evidence}}"), prompt);
        assertFalse(prompt.contains("Jamie"), prompt);
    }

    @Test
    void asksForJsonAndForbidsProbabilityClaims() {
        String prompt = builder.build(new ExplanationEvidenceFactory().from(ExplanationFixtures.evaluation()));

        assertTrue(prompt.contains("{\"explanation\""), prompt);
        assertTrue(prompt.contains("probability"), prompt);
    }
}
```

`ExplanationOutputValidatorTest.java`:

```java
package com.studybuddy.matchexplanation;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplanationOutputValidatorTest {
    private final ExplanationOutputValidator validator = new ExplanationOutputValidator(120, ExplanationFixtures.JSON);

    @Test
    void acceptsAShortGroundedExplanationAndTrimsIt() {
        assertEquals(Optional.of("You both take IS442 and share 3.5 free hours a week."),
                validator.validate("{\"explanation\": \"  You both take IS442 and share 3.5 free hours a week. \"}"));
    }

    @Test
    void rejectsMissingBlankOrNonJsonReplies() {
        assertTrue(validator.validate(null).isEmpty());
        assertTrue(validator.validate("").isEmpty());
        assertTrue(validator.validate("You both take IS442.").isEmpty());
        assertTrue(validator.validate("{\"explanation\": \"   \"}").isEmpty());
        assertTrue(validator.validate("{\"text\": \"You both take IS442.\"}").isEmpty());
        assertTrue(validator.validate("{\"explanation\": 42}").isEmpty());
    }

    @Test
    void rejectsRepliesOverTheLengthLimit() {
        assertTrue(validator.validate("{\"explanation\": \"" + "a".repeat(121) + "\"}").isEmpty());
    }

    @Test
    void rejectsSuccessClaimsAndPercentages() {
        for (String claim : new String[] {"There is a high probability you will get along.",
                "You have a good chance of success together.", "This pairing is guaranteed to work.",
                "You are 72% compatible."}) {
            assertTrue(validator.validate("{\"explanation\": \"" + claim + "\"}").isEmpty(), claim);
        }
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `cd backend && ./mvnw -q test -Dtest='ExplanationPromptBuilderTest,ExplanationOutputValidatorTest'`
Expected: compilation FAIL, classes not found.

- [ ] **Step 3: Create the prompt template** `backend/src/main/resources/prompts/match-explanation-v1.txt`

```text
You explain study-buddy match results to a university student.

Below is anonymous evidence about one match between "you" (the student reading) and "this student".
Scores run from 0 to 1. A criterion with "enabled": false did not count towards the total.
Quality is STRONG, GOOD or FAIR.

Write two or three short sentences that:
- start with the strongest enabled criteria, then mention the weakest enabled one;
- mention the shared hours per week and the course when they are present;
- use only facts in the evidence, and never invent names, preferences or details;
- never describe the result as a probability, chance of success, guarantee or percentage.

Reply with JSON only, in exactly this shape: {"explanation": "<your sentences>"}

Evidence:
{{evidence}}
```

- [ ] **Step 4: Create `ExplanationPromptBuilder`**

```java
package com.studybuddy.matchexplanation;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Fills the versioned prompt template with evidence JSON. The version is part of the cache key,
 * so editing the wording means adding a new template file and bumping {@link #PROMPT_VERSION}.
 */
@Component
public class ExplanationPromptBuilder {
    public static final String PROMPT_VERSION = "v1";
    private static final String TEMPLATE_PATH = "prompts/match-explanation-" + PROMPT_VERSION + ".txt";
    private static final String EVIDENCE_SLOT = "{{evidence}}";

    private final JsonMapper json;
    private final String template;

    public ExplanationPromptBuilder(JsonMapper json) {
        this.json = json;
        this.template = loadTemplate();
    }

    public String build(ExplanationEvidence evidence) {
        return template.replace(EVIDENCE_SLOT, json.writeValueAsString(evidence));
    }

    private static String loadTemplate() {
        try (InputStream input = new ClassPathResource(TEMPLATE_PATH).getInputStream()) {
            return StreamUtils.copyToString(input, StandardCharsets.UTF_8);
        } catch (IOException missing) {
            throw new IllegalStateException("Prompt template " + TEMPLATE_PATH + " is missing", missing);
        }
    }
}
```

- [ ] **Step 5: Create `ExplanationOutputValidator`.** It has no Spring annotation yet; Task 4 registers it as a bean.

```java
package com.studybuddy.matchexplanation;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Decides whether the provider's reply is safe to show. Anything malformed, too long or claiming
 * a likelihood of success is rejected, and the caller falls back to the standard explanation.
 */
public class ExplanationOutputValidator {
    /** Phrases that would present a compatibility score as a prediction. */
    private static final List<String> BANNED_PHRASES =
            List.of("probability", "chance of success", "guarantee", "likely to succeed", "%");

    private final int maxCharacters;
    private final JsonMapper json;

    public ExplanationOutputValidator(int maxCharacters, JsonMapper json) {
        this.maxCharacters = maxCharacters;
        this.json = json;
    }

    public Optional<String> validate(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        JsonNode reply;
        try {
            reply = json.readTree(raw);
        } catch (JacksonException notJson) {
            return Optional.empty();
        }
        JsonNode explanation = reply.path("explanation");
        if (!explanation.isString()) {
            return Optional.empty();
        }
        String text = explanation.asString().strip();
        if (text.isEmpty() || text.length() > maxCharacters || makesSuccessClaim(text)) {
            return Optional.empty();
        }
        return Optional.of(text);
    }

    private static boolean makesSuccessClaim(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return BANNED_PHRASES.stream().anyMatch(lower::contains);
    }
}
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `cd backend && ./mvnw -q test -Dtest='ExplanationPromptBuilderTest,ExplanationOutputValidatorTest'`
Expected: PASS (6 tests). If `isString()` does not compile, the Jackson 3 name for a text node check differs: run `grep -rn "isString\|isTextual" ~/.m2/repository/tools/jackson 2>/dev/null | head -1`, or check the Jackson 3 `JsonNode` Javadoc, and use the method it provides.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/resources/prompts backend/src/main/java/com/studybuddy/matchexplanation backend/src/test/java/com/studybuddy/matchexplanation
git commit -m "feat: add the versioned explanation prompt and reply validation"
```

---

### Task 4: Configuration, quota and bean wiring

**Files:**
- Create: `matchexplanation/AiExplanationProperties.java`, `matchexplanation/AiExplanationConfiguration.java`, `matchexplanation/ExplanationQuota.java`
- Modify: `backend/src/main/resources/application.yml` (under `app:`, after the `matching:` block), `README.md` (configuration table, after the `app.matching.*` rows)
- Test: `ExplanationQuotaTest.java`

**Interfaces:**
- Produces:
  - `record AiExplanationProperties(boolean enabled, String baseUrl, String model, String apiKey, Duration timeout, int maxOutputTokens, int maxCharacters, int dailyCallLimit, String quotaZone)` with `boolean ready()`.
  - `ExplanationQuota(AiExplanationProperties, Clock)` with `boolean tryAcquire()`.
  - A `Clock` bean and an `ExplanationOutputValidator` bean from `AiExplanationConfiguration`.

- [ ] **Step 1: Write the failing quota test**

```java
package com.studybuddy.matchexplanation;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplanationQuotaTest {
    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");

    private static AiExplanationProperties limit(int calls) {
        return new AiExplanationProperties(true, "http://localhost", "model", "key", Duration.ofSeconds(1),
                200, 600, calls, "Asia/Singapore");
    }

    private static final class MovableClock extends Clock {
        private Instant now;

        MovableClock(Instant start) {
            now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override public ZoneId getZone() { return SINGAPORE; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void allowsCallsUpToTheDailyLimit() {
        ExplanationQuota quota = new ExplanationQuota(limit(2), Clock.fixed(Instant.parse("2026-10-20T02:00:00Z"), SINGAPORE));

        assertTrue(quota.tryAcquire());
        assertTrue(quota.tryAcquire());
        assertFalse(quota.tryAcquire());
    }

    @Test
    void resetsAtSingaporeMidnight() {
        MovableClock clock = new MovableClock(Instant.parse("2026-10-20T15:30:00Z")); // 23:30 in Singapore
        ExplanationQuota quota = new ExplanationQuota(limit(1), clock);

        assertTrue(quota.tryAcquire());
        assertFalse(quota.tryAcquire());
        clock.advance(Duration.ofHours(1)); // 00:30 the next day in Singapore
        assertTrue(quota.tryAcquire());
    }

    @Test
    void aZeroLimitNeverCalls() {
        assertFalse(new ExplanationQuota(limit(0), Clock.systemUTC()).tryAcquire());
    }

    @Test
    void isReadyOnlyWhenEnabledWithAKey() {
        assertTrue(limit(1).ready());
        assertFalse(new AiExplanationProperties(false, "u", "m", "key", Duration.ofSeconds(1), 1, 1, 1, "UTC").ready());
        assertFalse(new AiExplanationProperties(true, "u", "m", null, Duration.ofSeconds(1), 1, 1, 1, "UTC").ready());
        assertFalse(new AiExplanationProperties(true, "u", "m", "  ", Duration.ofSeconds(1), 1, 1, 1, "UTC").ready());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd backend && ./mvnw -q test -Dtest=ExplanationQuotaTest`
Expected: compilation FAIL.

- [ ] **Step 3: Create `AiExplanationProperties`**

```java
package com.studybuddy.matchexplanation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * AI match explanation settings, bound from {@code app.ai.explanation.*}. The API key comes only
 * from the git-ignored {@code application-local.yml}; without it the feature stays off.
 */
@Validated
@ConfigurationProperties("app.ai.explanation")
public record AiExplanationProperties(
        boolean enabled,
        @NotBlank String baseUrl,
        @NotBlank String model,
        String apiKey,
        @NotNull Duration timeout,
        @Positive int maxOutputTokens,
        @Positive int maxCharacters,
        @PositiveOrZero int dailyCallLimit,
        @NotBlank String quotaZone) {

    /** True only when the feature is switched on and a key is configured. */
    public boolean ready() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /** Never print the key, even in a startup error. */
    @Override
    public String toString() {
        return "AiExplanationProperties[enabled=" + enabled + ", model=" + model + ", apiKey=" + (apiKey == null ? "absent" : "set") + "]";
    }
}
```

- [ ] **Step 4: Create `ExplanationQuota`**

```java
package com.studybuddy.matchexplanation;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Counts AI provider calls per day, so a busy demo falls back to the standard explanation
 * instead of exhausting the free tier. Kept in memory: a restart resets the count, which is
 * acceptable for a coursework deployment and documented in the README.
 */
@Component
public class ExplanationQuota {
    private final int dailyLimit;
    private final Clock clock;
    private LocalDate day;
    private int used;

    public ExplanationQuota(AiExplanationProperties properties, Clock explanationQuotaClock) {
        this.dailyLimit = properties.dailyCallLimit();
        this.clock = explanationQuotaClock;
    }

    public synchronized boolean tryAcquire() {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(day)) {
            day = today;
            used = 0;
        }
        if (used >= dailyLimit) {
            return false;
        }
        used++;
        return true;
    }
}
```

- [ ] **Step 5: Create `AiExplanationConfiguration`**

```java
package com.studybuddy.matchexplanation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableConfigurationProperties(AiExplanationProperties.class)
public class AiExplanationConfiguration {

    /** The day boundary for the call quota follows the students' time zone, not the server's. */
    @Bean
    public Clock explanationQuotaClock(AiExplanationProperties properties) {
        return Clock.system(ZoneId.of(properties.quotaZone()));
    }

    @Bean
    public ExplanationOutputValidator explanationOutputValidator(AiExplanationProperties properties, JsonMapper json) {
        return new ExplanationOutputValidator(properties.maxCharacters(), json);
    }
}
```

- [ ] **Step 6: Add the config block to `application.yml`**, indented under `app:` at the same level as `matching:`:

```yaml
  ai:
    explanation:
      # Off unless application-local.yml sets enabled: true and api-key. Never put the key here.
      enabled: false
      base-url: https://generativelanguage.googleapis.com/v1beta
      model: gemini-2.5-flash
      timeout: 5s
      max-output-tokens: 200
      max-characters: 600
      daily-call-limit: 200
      quota-zone: Asia/Singapore
```

Set `model` and `daily-call-limit` from what you checked in "Before you start". Keep the limit below the free tier's daily cap.

- [ ] **Step 7: Add README configuration rows** after the last `app.matching.*` row in the table at `README.md:265`:

```markdown
| `app.ai.explanation.enabled` | `false` | Turns on Gemini match explanations; also needs `api-key`. Off means the standard explanation is always used |
| `app.ai.explanation.api-key` | none; set only in `application-local.yml` | Gemini API key. Never committed, logged or sent to the browser |
| `app.ai.explanation.base-url` | `https://generativelanguage.googleapis.com/v1beta` | Gemini API base URL |
| `app.ai.explanation.model` | `gemini-2.5-flash` | Gemini model ID; part of the explanation cache key |
| `app.ai.explanation.timeout` | `5s` | Connect and read timeout before falling back to the standard explanation |
| `app.ai.explanation.max-output-tokens` | `200` | Upper bound on Gemini's reply length |
| `app.ai.explanation.max-characters` | `600` | Longest explanation shown; longer replies fall back |
| `app.ai.explanation.daily-call-limit` | `200` | Gemini calls per day before falling back; in memory, so a restart resets it |
| `app.ai.explanation.quota-zone` | `Asia/Singapore` | Time zone whose midnight resets the daily limit |
```

- [ ] **Step 8: Run the tests to verify they pass**

Run: `cd backend && ./mvnw -q test -Dtest='ExplanationQuotaTest,ExplanationOutputValidatorTest'`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/studybuddy/matchexplanation backend/src/test/java/com/studybuddy/matchexplanation backend/src/main/resources/application.yml README.md
git commit -m "feat: configure ai explanations with a daily call quota"
```

---

### Task 5: Gemini provider

**Files:**
- Create: `matchexplanation/ExplanationProvider.java`, `matchexplanation/GeminiExplanationProvider.java`
- Test: `GeminiExplanationProviderTest.java`

**Interfaces:**
- Consumes: `AiExplanationProperties` (Task 4), `JsonMapper`.
- Produces: `interface ExplanationProvider { Optional<String> explain(String prompt); }`. `GeminiExplanationProvider` returns the model's raw reply text, which Task 7 hands to the validator.

- [ ] **Step 1: Write the failing test against a local stub server**

```java
package com.studybuddy.matchexplanation;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiExplanationProviderTest {
    private static final String REPLY = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"explanation\\\":\\\"You both take IS442.\\\"}\"}]}}]}";

    private HttpServer server;
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> requestKey = new AtomicReference<>();
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private volatile int status = 200;
    private volatile long delayMillis = 0;
    private volatile String reply = REPLY;

    @BeforeEach
    void startStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            requestKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = reply.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    @AfterEach
    void stopStub() {
        server.stop(0);
    }

    private GeminiExplanationProvider provider() {
        return new GeminiExplanationProvider(new AiExplanationProperties(true,
                "http://127.0.0.1:" + server.getAddress().getPort(), "test-model", "test-key", Duration.ofMillis(500),
                200, 600, 10, "UTC"), ExplanationFixtures.JSON);
    }

    @Test
    void returnsTheModelsTextAndSendsOnlyThePromptAndSettings() {
        assertEquals(Optional.of("{\"explanation\":\"You both take IS442.\"}"), provider().explain("PROMPT-TEXT"));

        assertEquals("/models/test-model:generateContent", requestPath.get());
        assertEquals("test-key", requestKey.get());
        String body = requestBody.get();
        assertTrue(body.contains("PROMPT-TEXT"), body);
        assertTrue(body.contains("\"responseMimeType\":\"application/json\""), body);
        assertTrue(body.contains("\"maxOutputTokens\":200"), body);
        assertFalse(body.contains("test-key"), body);
    }

    @Test
    void serverErrorReturnsEmpty() {
        status = 500;
        assertTrue(provider().explain("PROMPT").isEmpty());
    }

    @Test
    void slowReplyReturnsEmpty() {
        delayMillis = 1500;
        assertTrue(provider().explain("PROMPT").isEmpty());
    }

    @Test
    void replyWithoutCandidatesReturnsEmpty() {
        reply = "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}";
        assertTrue(provider().explain("PROMPT").isEmpty());
    }

    @Test
    void unreadableReplyReturnsEmpty() {
        reply = "not json";
        assertTrue(provider().explain("PROMPT").isEmpty());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd backend && ./mvnw -q test -Dtest=GeminiExplanationProviderTest`
Expected: compilation FAIL.

- [ ] **Step 3: Create `ExplanationProvider`**

```java
package com.studybuddy.matchexplanation;

import java.util.Optional;

/** Writes an explanation from a prompt. Empty means no usable answer; it never throws. */
public interface ExplanationProvider {
    Optional<String> explain(String prompt);
}
```

- [ ] **Step 4: Create `GeminiExplanationProvider`**

```java
package com.studybuddy.matchexplanation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Calls Google Gemini's generateContent endpoint. Any failure (timeout, HTTP error, blocked or
 * unreadable reply) is logged without the prompt or key and returned as empty.
 */
@Component
public class GeminiExplanationProvider implements ExplanationProvider {
    private static final Logger LOG = LoggerFactory.getLogger(GeminiExplanationProvider.class);

    private final AiExplanationProperties properties;
    private final JsonMapper json;
    private final RestClient client;

    public GeminiExplanationProvider(AiExplanationProperties properties, JsonMapper json) {
        this.properties = properties;
        this.json = json;
        HttpClient http = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(http);
        requests.setReadTimeout(properties.timeout());
        this.client = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(requests).build();
    }

    @Override
    public Optional<String> explain(String prompt) {
        try {
            String reply = client.post()
                    .uri("/models/{model}:generateContent", properties.model())
                    .header("x-goog-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsString(requestFor(prompt)))
                    .retrieve()
                    .body(String.class);
            return textOf(reply);
        } catch (RestClientException | JacksonException failure) {
            LOG.warn("Gemini explanation unavailable: {}", failure.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private Map<String, Object> requestFor(String prompt) {
        return Map.of(
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "maxOutputTokens", properties.maxOutputTokens()));
    }

    private Optional<String> textOf(String reply) {
        if (reply == null) {
            return Optional.empty();
        }
        JsonNode text = json.readTree(reply).path("candidates").path(0).path("content").path("parts").path(0).path("text");
        return text.isString() && !text.asString().isBlank() ? Optional.of(text.asString()) : Optional.empty();
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd backend && ./mvnw -q test -Dtest=GeminiExplanationProviderTest`
Expected: PASS (5 tests). If `slowReplyReturnsEmpty` hangs past 1.5s, the read timeout is not applied: confirm `JdkClientHttpRequestFactory#setReadTimeout(Duration)` exists in Spring Framework 7.0.9. It does in 6.1+.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/studybuddy/matchexplanation backend/src/test/java/com/studybuddy/matchexplanation
git commit -m "feat: call gemini for match explanations with a timeout and safe failure"
```

---

### Task 6: Explanation cache table, entity and repository

**Files:**
- Create: `backend/src/main/resources/db/migrations/20261016090000_match_explanation_cache.sql`, `matchexplanation/MatchExplanation.java`, `matchexplanation/MatchExplanationRepository.java`, `matchexplanation/ExplanationCache.java`
- Modify: `backend/src/test/java/com/studybuddy/integration/PostgresHttpTest.java:52` (truncate list), `backend/src/test/java/com/studybuddy/integration/DatabaseBoundaryTest.java` (query list)
- Test: `backend/src/test/java/com/studybuddy/integration/MatchExplanationHttpTest.java` (cache test only; Task 8 adds HTTP tests)

**Interfaces:**
- Produces: `ExplanationCache#find(String hash) : Optional<String>` and `ExplanationCache#save(String hash, String promptVersion, String model, String text) : void` (a duplicate is silently ignored).

- [ ] **Step 1: Write the failing integration test**

```java
package com.studybuddy.integration;

import com.studybuddy.matchexplanation.ExplanationCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AI explanation cache and endpoint, over real PostgreSQL and HTTP. */
class MatchExplanationHttpTest extends PostgresHttpTest {
    private static final String HASH = "a".repeat(64);

    @Autowired
    private ExplanationCache cache;

    @Test
    void savingTheSameEvidenceTwiceKeepsOneRow() {
        assertTrue(cache.find(HASH).isEmpty());

        cache.save(HASH, "v1", "test-model", "First answer.");
        cache.save(HASH, "v1", "test-model", "Second answer.");

        assertEquals(Optional.of("First answer."), cache.find(HASH));
        assertEquals(1, count("select count(*) from match_explanations"));
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `scripts/test-backend.sh -Dtest=MatchExplanationHttpTest`
Expected: compilation FAIL, `ExplanationCache` not found.

- [ ] **Step 3: Create the migration** `20261016090000_match_explanation_cache.sql`. It is repeat-safe and backend-only, like `20261013090000_complete_matching_configuration.sql`.

```sql
-- Cached AI match explanations, keyed by a hash of anonymous evidence. Holds no student IDs,
-- so account deletion and deactivation need no cleanup here. Backend-only, like every table.
CREATE TABLE IF NOT EXISTS public.match_explanations (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    evidence_hash varchar(64) NOT NULL,
    prompt_version varchar(16) NOT NULL,
    model varchar(64) NOT NULL,
    explanation_text text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE public.match_explanations DROP CONSTRAINT IF EXISTS match_explanations_evidence_hash_key;
ALTER TABLE public.match_explanations ADD CONSTRAINT match_explanations_evidence_hash_key UNIQUE (evidence_hash);
ALTER TABLE public.match_explanations DROP CONSTRAINT IF EXISTS match_explanations_hash_format_check;
ALTER TABLE public.match_explanations ADD CONSTRAINT match_explanations_hash_format_check CHECK (evidence_hash ~ '^[0-9a-f]{64}$');
ALTER TABLE public.match_explanations ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.match_explanations FROM PUBLIC,anon,authenticated;
REVOKE ALL ON SEQUENCE public.match_explanations_id_seq FROM PUBLIC,anon,authenticated;
```

On `main` the running app uses `ddl-auto: update`, so locally Hibernate would also create this table. The migration is still the source of truth: tests run with `ddl-auto: validate`, and shared Supabase only gets tables through migrations.

- [ ] **Step 4: Create the entity** `MatchExplanation.java`. It is read-only in code; inserts go through the repository's native query.

```java
package com.studybuddy.matchexplanation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** One cached explanation. Never leaves the service layer; the API sees only its text. */
@Entity
@Table(name = "match_explanations")
public class MatchExplanation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evidence_hash", nullable = false, length = 64, unique = true)
    private String evidenceHash;

    @Column(name = "prompt_version", nullable = false, length = 16)
    private String promptVersion;

    @Column(nullable = false, length = 64)
    private String model;

    @Column(name = "explanation_text", nullable = false, columnDefinition = "text")
    private String explanationText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MatchExplanation() {
    }

    public String getExplanationText() {
        return explanationText;
    }
}
```

- [ ] **Step 5: Create the repository**

```java
package com.studybuddy.matchexplanation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface MatchExplanationRepository extends JpaRepository<MatchExplanation, Long> {
    Optional<MatchExplanation> findByEvidenceHash(String evidenceHash);

    /** Two students clicking at once may both write the same evidence; the second insert is ignored. */
    @Transactional
    @Modifying
    @Query(value = "insert into match_explanations(evidence_hash,prompt_version,model,explanation_text,created_at) "
            + "values (:hash,:promptVersion,:model,:text,now()) on conflict(evidence_hash) do nothing", nativeQuery = true)
    void insertIfAbsent(@Param("hash") String hash, @Param("promptVersion") String promptVersion,
            @Param("model") String model, @Param("text") String text);
}
```

- [ ] **Step 6: Create `ExplanationCache`**

```java
package com.studybuddy.matchexplanation;

import org.springframework.stereotype.Component;

import java.util.Optional;

/** Reads and writes cached explanation text by evidence hash, keeping the entity out of the service. */
@Component
public class ExplanationCache {
    private final MatchExplanationRepository repository;

    public ExplanationCache(MatchExplanationRepository repository) {
        this.repository = repository;
    }

    public Optional<String> find(String hash) {
        return repository.findByEvidenceHash(hash).map(MatchExplanation::getExplanationText);
    }

    public void save(String hash, String promptVersion, String model, String text) {
        repository.insertIfAbsent(hash, promptVersion, model, text);
    }
}
```

- [ ] **Step 7: Include the table in the test reset and the browser-role check**

In `PostgresHttpTest.java:52`, change the truncate statement to:

```java
        database.execute("truncate users, courses, matching_configs, matching_strategy_settings, match_explanations restart identity cascade");
```

In `DatabaseBoundaryTest.java`, add `"select * from match_explanations"` to the `query` array in `anonymousAndAuthenticatedBrowserRolesCannotReadOrWriteApplicationTables`, after `"select * from notifications"`.

- [ ] **Step 8: Run the tests to verify they pass**

Run: `scripts/test-backend.sh -Dtest='MatchExplanationHttpTest,DatabaseBoundaryTest,StudyBuddyApplicationTests'`
Expected: PASS. `StudyBuddyApplicationTests` confirms Hibernate's `validate` accepts the entity against the migrated table.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/resources/db/migrations/20261016090000_match_explanation_cache.sql backend/src/main/java/com/studybuddy/matchexplanation backend/src/test/java/com/studybuddy/integration
git commit -m "feat: cache ai match explanations by evidence hash"
```

---

### Task 7: Explanation service

**Files:**
- Create: `matchexplanation/ExplanationSource.java`, `matchexplanation/MatchExplanationDto.java`, `matchexplanation/MatchExplanationAssembler.java`, `matchexplanation/MatchExplanationService.java`
- Test: `MatchExplanationServiceTest.java`

**Interfaces:**
- Consumes: `MatchService#evaluate` (Task 1), `ExplanationEvidenceFactory`, `ExplanationHasher` (Task 2), `ExplanationPromptBuilder`, `ExplanationOutputValidator` (Task 3), `AiExplanationProperties`, `ExplanationQuota` (Task 4), `ExplanationProvider` (Task 5), `ExplanationCache` (Task 6).
- Produces: `MatchExplanationService#explain(MatchSearchCriteria search, Long actorId, Long candidateId) : MatchExplanationDto`; `record MatchExplanationDto(Long candidateId, String text, ExplanationSource source)`.

- [ ] **Step 1: Write the failing test**

```java
package com.studybuddy.matchexplanation;

import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.matching.MatchService;
import com.studybuddy.student.StudentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchExplanationServiceTest {
    private static final long ME = 1L;
    private static final long PEER = ExplanationFixtures.CANDIDATE_ID;
    private static final MatchSearchCriteria SEARCH = new MatchSearchCriteria(4L, null, null, null, null, null);
    private static final String GOOD_REPLY = "{\"explanation\":\"You both take IS442.\"}";

    @Mock private MatchService matches;
    @Mock private ExplanationCache cache;
    @Mock private ExplanationQuota quota;
    @Mock private ExplanationProvider provider;

    private final ExplanationHasher hasher = new ExplanationHasher(ExplanationFixtures.JSON);
    private final ExplanationEvidenceFactory evidence = new ExplanationEvidenceFactory();
    private String hash;

    private MatchExplanationService service(boolean ready) {
        AiExplanationProperties properties = new AiExplanationProperties(ready, "http://localhost", "test-model",
                ready ? "key" : null, Duration.ofSeconds(1), 200, 600, 10, "UTC");
        return new MatchExplanationService(matches, evidence, hasher, cache, quota,
                new ExplanationPromptBuilder(ExplanationFixtures.JSON), provider,
                new ExplanationOutputValidator(600, ExplanationFixtures.JSON), new MatchExplanationAssembler(), properties);
    }

    @BeforeEach
    void setUp() {
        when(matches.evaluate(SEARCH, ME, PEER)).thenReturn(ExplanationFixtures.evaluation());
        hash = hasher.hash(evidence.from(ExplanationFixtures.evaluation()), ExplanationPromptBuilder.PROMPT_VERSION, "test-model");
        when(cache.find(hash)).thenReturn(Optional.empty());
        when(quota.tryAcquire()).thenReturn(true);
        when(provider.explain(anyString())).thenReturn(Optional.of(GOOD_REPLY));
    }

    @Test
    void disabledFeatureReturnsTheStandardTextWithoutCallingAnything() {
        MatchExplanationDto result = service(false).explain(SEARCH, ME, PEER);

        assertEquals(new MatchExplanationDto(PEER, "good match: same course", ExplanationSource.STANDARD), result);
        verifyNoInteractions(cache, quota, provider);
    }

    @Test
    void cacheHitSkipsTheProvider() {
        when(cache.find(hash)).thenReturn(Optional.of("Cached answer."));

        assertEquals(new MatchExplanationDto(PEER, "Cached answer.", ExplanationSource.AI), service(true).explain(SEARCH, ME, PEER));
        verifyNoInteractions(provider, quota);
    }

    @Test
    void aGoodAnswerIsReturnedAndCached() {
        assertEquals(new MatchExplanationDto(PEER, "You both take IS442.", ExplanationSource.AI), service(true).explain(SEARCH, ME, PEER));
        verify(cache).save(hash, ExplanationPromptBuilder.PROMPT_VERSION, "test-model", "You both take IS442.");
    }

    @Test
    void noAnswerFallsBackAndCachesNothing() {
        when(provider.explain(anyString())).thenReturn(Optional.empty());

        assertEquals(ExplanationSource.STANDARD, service(true).explain(SEARCH, ME, PEER).source());
        verify(cache, never()).save(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void aRejectedAnswerFallsBackAndCachesNothing() {
        when(provider.explain(anyString())).thenReturn(Optional.of("{\"explanation\":\"A 90% chance of success.\"}"));

        assertEquals(ExplanationSource.STANDARD, service(true).explain(SEARCH, ME, PEER).source());
        verify(cache, never()).save(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void anExhaustedQuotaFallsBackWithoutCallingTheProvider() {
        when(quota.tryAcquire()).thenReturn(false);

        assertEquals(ExplanationSource.STANDARD, service(true).explain(SEARCH, ME, PEER).source());
        verifyNoInteractions(provider);
    }

    @Test
    void searchErrorsPassThrough() {
        when(matches.evaluate(SEARCH, ME, 9L)).thenThrow(new StudentNotFoundException(9L));

        assertThrows(StudentNotFoundException.class, () -> service(true).explain(SEARCH, ME, 9L));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd backend && ./mvnw -q test -Dtest=MatchExplanationServiceTest`
Expected: compilation FAIL.

- [ ] **Step 3: Create the DTO, source enum and assembler**

`ExplanationSource.java`:

```java
package com.studybuddy.matchexplanation;

/** Where an explanation's text came from, so the screen can label it honestly. */
public enum ExplanationSource {
    /** Written by the AI provider from anonymous evidence. */
    AI,
    /** The matching engine's own deterministic explanation. */
    STANDARD
}
```

`MatchExplanationDto.java`:

```java
package com.studybuddy.matchexplanation;

/** An explanation of one match, safe to send to the browser. */
public record MatchExplanationDto(Long candidateId, String text, ExplanationSource source) {
}
```

`MatchExplanationAssembler.java`:

```java
package com.studybuddy.matchexplanation;

import org.springframework.stereotype.Component;

@Component
public class MatchExplanationAssembler {
    public MatchExplanationDto ai(Long candidateId, String text) {
        return new MatchExplanationDto(candidateId, text, ExplanationSource.AI);
    }

    public MatchExplanationDto standard(Long candidateId, String text) {
        return new MatchExplanationDto(candidateId, text, ExplanationSource.STANDARD);
    }
}
```

- [ ] **Step 4: Create `MatchExplanationService`.** It is deliberately not `@Transactional`: `MatchService.evaluate` and the repository each run their own short transaction, so no database connection is held during the Gemini call.

```java
package com.studybuddy.matchexplanation;

import com.studybuddy.matching.MatchEvaluation;
import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.matching.MatchService;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Explains one match. Scores come from the matching engine unchanged; the AI provider only
 * words them. Whenever the provider is off, over quota, failing or gives an unusable answer,
 * the matching engine's own explanation is returned instead, so this never fails because AI did.
 */
@Service
public class MatchExplanationService {
    private final MatchService matches;
    private final ExplanationEvidenceFactory evidenceFactory;
    private final ExplanationHasher hasher;
    private final ExplanationCache cache;
    private final ExplanationQuota quota;
    private final ExplanationPromptBuilder prompts;
    private final ExplanationProvider provider;
    private final ExplanationOutputValidator validator;
    private final MatchExplanationAssembler assembler;
    private final AiExplanationProperties properties;

    public MatchExplanationService(MatchService matches, ExplanationEvidenceFactory evidenceFactory,
            ExplanationHasher hasher, ExplanationCache cache, ExplanationQuota quota, ExplanationPromptBuilder prompts,
            ExplanationProvider provider, ExplanationOutputValidator validator, MatchExplanationAssembler assembler,
            AiExplanationProperties properties) {
        this.matches = matches;
        this.evidenceFactory = evidenceFactory;
        this.hasher = hasher;
        this.cache = cache;
        this.quota = quota;
        this.prompts = prompts;
        this.provider = provider;
        this.validator = validator;
        this.assembler = assembler;
        this.properties = properties;
    }

    public MatchExplanationDto explain(MatchSearchCriteria search, Long actorId, Long candidateId) {
        MatchEvaluation evaluation = matches.evaluate(search, actorId, candidateId);
        String standardText = evaluation.result().explanation();
        if (!properties.ready()) {
            return assembler.standard(candidateId, standardText);
        }
        ExplanationEvidence evidence = evidenceFactory.from(evaluation);
        String hash = hasher.hash(evidence, ExplanationPromptBuilder.PROMPT_VERSION, properties.model());
        Optional<String> cached = cache.find(hash);
        if (cached.isPresent()) {
            return assembler.ai(candidateId, cached.get());
        }
        if (!quota.tryAcquire()) {
            return assembler.standard(candidateId, standardText);
        }
        Optional<String> written = provider.explain(prompts.build(evidence)).flatMap(validator::validate);
        if (written.isEmpty()) {
            return assembler.standard(candidateId, standardText);
        }
        cache.save(hash, ExplanationPromptBuilder.PROMPT_VERSION, properties.model(), written.get());
        return assembler.ai(candidateId, written.get());
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd backend && ./mvnw -q test -Dtest=MatchExplanationServiceTest`
Expected: PASS (7 tests).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/studybuddy/matchexplanation backend/src/test/java/com/studybuddy/matchexplanation
git commit -m "feat: explain a match with ai and fall back to the standard text"
```

---

### Task 8: Endpoint, HTTP tests and API contract

**Files:**
- Create: `matchexplanation/MatchExplanationController.java`
- Modify: `backend/src/test/java/com/studybuddy/integration/MatchExplanationHttpTest.java` (add a test), `docs/API_CONTRACT.md` (new section after "## Match search (Team A)")

**Interfaces:**
- Consumes: `MatchExplanationService#explain` (Task 7).
- Produces: `GET /api/matches/{candidateId}/explanation`, returning `{candidateId, text, source}`.

- [ ] **Step 1: Write the failing HTTP test.** Append to `MatchExplanationHttpTest`, adding imports for `tools.jackson.databind.JsonNode` and `static org.junit.jupiter.api.Assertions.assertFalse`. The test profile has no API key, so the feature is off and every answer is `STANDARD`.

```java
    @Test
    void explanationsFollowSearchRulesAndFallBackWhenAiIsOff() {
        long self = student("self@example.test", "Self", "81112222");
        long peer = student("peer@example.test", "Peer", "83334444");
        long inactive = student("inactive@example.test", "Inactive", "85556666");
        database.update("update users set active=false where id=?", inactive);
        account("admin@example.test", "ADMIN");
        String token = login("self@example.test");
        String adminToken = login("admin@example.test");
        String context = "/explanation?courseId=" + course;

        expect(call("GET", "/matches/" + peer + context, null, null), 401);
        expect(call("GET", "/matches/" + peer + context, adminToken, null), 403);
        expect(call("GET", "/matches/" + self + context, token, null), 404);
        expect(call("GET", "/matches/" + inactive + context, token, null), 404);
        expect(call("GET", "/matches/99999999" + context, token, null), 404);
        expect(call("GET", "/matches/" + peer + "/explanation", token, null), 400);

        Reply reply = call("GET", "/matches/" + peer + context, token, null);
        JsonNode explanation = expect(reply, 200);
        JsonNode card = expect(call("GET", "/matches?courseId=" + course, token, null), 200).path("matches").get(0);
        assertEquals(peer, explanation.path("candidateId").asLong());
        assertEquals("STANDARD", explanation.path("source").asString());
        assertEquals(card.path("explanation").asString(), explanation.path("text").asString());
        for (String forbidden : new String[] {"contactNumber", "email", "83334444", "Peer"}) {
            assertFalse(reply.body().contains(forbidden), reply.body());
        }
        assertEquals(0, count("select count(*) from match_explanations"));
    }
```

- [ ] **Step 2: Run it to verify it fails**

Run: `scripts/test-backend.sh -Dtest=MatchExplanationHttpTest`
Expected: FAIL. The new test receives 404 for the 200 case (no route), or 401/403 mismatches.

- [ ] **Step 3: Create the controller.** Same parameters as `MatchController#search` except `sort` and `limit`, which do not affect a single score.

```java
package com.studybuddy.matchexplanation;

import com.studybuddy.matching.MatchQuality;
import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.security.AccountPrincipal;
import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Explains one match on request. Takes the same search context as {@code GET /api/matches}
 * so the explanation describes exactly the score the student saw.
 */
@RestController
@RequestMapping("/api/matches")
public class MatchExplanationController {
    private final MatchExplanationService service;

    public MatchExplanationController(MatchExplanationService service) {
        this.service = service;
    }

    @GetMapping("/{candidateId}/explanation")
    public MatchExplanationDto explain(@AuthenticationPrincipal AccountPrincipal actor,
            @PathVariable Long candidateId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) StudyGoal studyGoal,
            @RequestParam(required = false) StudyMode studyMode,
            @RequestParam(required = false) GroupSizePreference groupSize,
            @RequestParam(required = false) Integer minSharedHours,
            @RequestParam(required = false) MatchQuality minQuality) {
        return service.explain(new MatchSearchCriteria(courseId, studyGoal, studyMode, groupSize, minSharedHours, minQuality),
                actor.id(), candidateId);
    }
}
```

No security change is needed: `SecurityConfiguration` already requires the `STUDENT` role for every `/api/**` path outside `/api/admin/**`, which gives the 401 and 403 cases.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `scripts/test-backend.sh -Dtest='MatchExplanationHttpTest,MatchingHttpTest'`
Expected: PASS, both explanation tests and the unchanged search tests.

- [ ] **Step 5: Record the endpoint in `docs/API_CONTRACT.md`.** Insert this section immediately before `## UI use of the existing contract`:

```markdown
## Match explanation (E1)

Student only. `GET /api/matches/{candidateId}/explanation` returns a short explanation of one
match, fetched only when the student clicks **Explain this match**. It accepts the same
`courseId`, `studyGoal`, `studyMode`, `groupSize`, `minSharedHours` and `minQuality` parameters
as `GET /api/matches` (no `sort` or `limit`) and validates them the same way, so it explains the
score on the card that was clicked.

```json
{ "candidateId": 42, "text": "You both take IS442 ...", "source": "AI" }
```

`source` is `AI` when Gemini wrote the text from anonymous evidence, or `STANDARD` when the
matching engine's own explanation is returned. Standard is returned when the feature is off,
the daily call limit is reached, or Gemini fails, times out or answers unusably. Errors:
401 signed out, 403 not a student, 400 invalid search, 404 for an unknown, inactive or own
candidate. Only the fields of `ExplanationEvidence` (quality, total, per-criterion scores,
strategy, shared hours, course code and name, shared goals, both study modes and group sizes)
are ever sent to Gemini. Never names, IDs, contact details, school, year or schedules.
```

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/studybuddy/matchexplanation backend/src/test/java/com/studybuddy/integration/MatchExplanationHttpTest.java docs/API_CONTRACT.md
git commit -m "feat: add the match explanation endpoint"
```

---

### Task 9: Frontend explanation panel on the match card

**Files:**
- Create: `frontend/src/features/matchexplanation/api.ts`, `frontend/src/features/matchexplanation/MatchExplanationPanel.tsx`, `frontend/src/features/matchexplanation/MatchExplanationPanel.test.tsx`
- Modify: `frontend/src/features/matching/MatchCard.tsx` (props and one line after `.match-explanation`), `frontend/src/features/matching/FindBuddiesPanel.tsx:143` (pass `search`)

**Interfaces:**
- Consumes: `api` from `src/shared/api/client`, `MatchSearch` from `src/features/matching/api`, and the endpoint from Task 8.
- Produces: `fetchMatchExplanation(candidateId: number, search: MatchSearch, signal?: AbortSignal): Promise<MatchExplanation>`; `<MatchExplanationPanel candidateId={number} search={MatchSearch} />`.

- [ ] **Step 1: Write the failing tests** `MatchExplanationPanel.test.tsx`

```tsx
import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { api } from '../../shared/api/client'
import { deferred, response } from '../../test/renderApp'
import MatchExplanationPanel from './MatchExplanationPanel'

const search = { courseId: 4, studyMode: 'ONLINE' as const, sort: 'MOST_SHARED_HOURS' as const, limit: 20 }
afterEach(() => { vi.restoreAllMocks() })

describe('MatchExplanationPanel', () => {
  it('fetches only when clicked, with the search context but not sort or limit', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue(response({ candidateId: 2, text: 'You both take IS442.', source: 'AI' }))
    render(<MatchExplanationPanel candidateId={2} search={search} />)
    expect(get).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: 'Explain this match' }))

    expect(await screen.findByText('You both take IS442.')).toBeTruthy()
    expect(get).toHaveBeenCalledWith('/matches/2/explanation', expect.objectContaining({ params: { courseId: 4, studyMode: 'ONLINE' } }))
  })

  it('shows a loading state while the explanation is written', async () => {
    const pending = deferred<unknown>()
    vi.spyOn(api, 'get').mockReturnValue(pending.promise as never)
    render(<MatchExplanationPanel candidateId={2} search={search} />)

    fireEvent.click(screen.getByRole('button', { name: 'Explain this match' }))

    expect((screen.getByRole('button', { name: 'Writing an explanation…' }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('labels AI text as AI-generated and standard text as standard', async () => {
    vi.spyOn(api, 'get').mockResolvedValueOnce(response({ candidateId: 2, text: 'AI words.', source: 'AI' }))
    const { unmount } = render(<MatchExplanationPanel candidateId={2} search={search} />)
    fireEvent.click(screen.getByRole('button', { name: 'Explain this match' }))
    expect(await screen.findByText(/AI-generated by Gemini/)).toBeTruthy()
    unmount()

    vi.spyOn(api, 'get').mockResolvedValueOnce(response({ candidateId: 2, text: 'Plain words.', source: 'STANDARD' }))
    render(<MatchExplanationPanel candidateId={2} search={search} />)
    fireEvent.click(screen.getByRole('button', { name: 'Explain this match' }))
    expect(await screen.findByText('Standard explanation')).toBeTruthy()
  })

  it('offers a retry after an error', async () => {
    vi.spyOn(api, 'get').mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce(response({ candidateId: 2, text: 'Second try.', source: 'AI' }))
    render(<MatchExplanationPanel candidateId={2} search={search} />)

    fireEvent.click(screen.getByRole('button', { name: 'Explain this match' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Try again' }))

    expect(await screen.findByText('Second try.')).toBeTruthy()
  })

  it('rendersTextLiterally', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response({ candidateId: 2, text: '<b>bold</b> <script>x</script>', source: 'AI' }))
    const { container } = render(<MatchExplanationPanel candidateId={2} search={search} />)

    fireEvent.click(screen.getByRole('button', { name: 'Explain this match' }))

    expect(await screen.findByText('<b>bold</b> <script>x</script>')).toBeTruthy()
    expect(container.querySelector('b, script')).toBeNull()
  })
})
```

Check `deferred` in `src/test/renderApp.tsx` returns `{ promise, resolve, reject }`. If its shape differs, adapt the loading test to it.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `cd frontend && npx vitest run src/features/matchexplanation`
Expected: FAIL, cannot resolve `./MatchExplanationPanel`.

- [ ] **Step 3: Create `api.ts`**

```ts
import { api } from '../../shared/api/client'
import type { MatchSearch } from '../matching/api'

export type ExplanationSource = 'AI' | 'STANDARD'
export type MatchExplanation = { candidateId: number; text: string; source: ExplanationSource }

/** Sends the search context that produced the card, so the explanation describes the same score. */
export async function fetchMatchExplanation(candidateId: number, search: MatchSearch, signal?: AbortSignal) {
  const { courseId, studyGoal, studyMode, groupSize, minSharedHours, minQuality } = search
  const params = Object.fromEntries(Object.entries({ courseId, studyGoal, studyMode, groupSize, minSharedHours, minQuality })
    .filter(([, value]) => value !== undefined))
  return (await api.get<MatchExplanation>(`/matches/${candidateId}/explanation`, { params, signal })).data
}
```

- [ ] **Step 4: Create `MatchExplanationPanel.tsx`**

```tsx
import { useState } from 'react'
import Button from '../../shared/components/Button'
import type { MatchSearch } from '../matching/api'
import { fetchMatchExplanation, type ExplanationSource, type MatchExplanation } from './api'

type PanelState = { status: 'idle' } | { status: 'loading' } | { status: 'error' } | { status: 'done'; explanation: MatchExplanation }

const SOURCE_LABELS: Record<ExplanationSource, string> = {
  AI: 'AI-generated by Gemini. Scores are calculated separately and are not changed by this text.',
  STANDARD: 'Standard explanation'
}

/** Asks for a written explanation of one match only when the student wants it. */
export default function MatchExplanationPanel({ candidateId, search }: { candidateId: number; search: MatchSearch }) {
  const [state, setState] = useState<PanelState>({ status: 'idle' })

  async function load() {
    setState({ status: 'loading' })
    try {
      setState({ status: 'done', explanation: await fetchMatchExplanation(candidateId, search) })
    } catch {
      setState({ status: 'error' })
    }
  }

  if (state.status === 'done') {
    return (
      <div className="match-ai-explanation" aria-live="polite">
        <p>{state.explanation.text}</p>
        <p className="field-hint">{SOURCE_LABELS[state.explanation.source]}</p>
      </div>
    )
  }
  if (state.status === 'error') {
    return <p className="field-hint" role="alert">Couldn't load an explanation. <Button onClick={load}>Try again</Button></p>
  }
  const loading = state.status === 'loading'
  return <Button onClick={load} disabled={loading}>{loading ? 'Writing an explanation…' : 'Explain this match'}</Button>
}
```

- [ ] **Step 5: Run the panel tests to verify they pass**

Run: `cd frontend && npx vitest run src/features/matchexplanation`
Expected: PASS (5 tests).

- [ ] **Step 6: Wire it into the match card (Team A files; only with their agreement)**

In `MatchCard.tsx`, change the imports and signature:

```tsx
import type { MatchResult, MatchSearch } from './api'
import MatchExplanationPanel from '../matchexplanation/MatchExplanationPanel'
```

```tsx
export default function MatchCard({ match, search, onSendRequest }: { match: MatchResult; search: MatchSearch; onSendRequest: () => void }) {
```

Directly after `<p className="match-explanation">{match.explanation}</p>` add:

```tsx
        <MatchExplanationPanel candidateId={match.studentId} search={search} />
```

In `FindBuddiesPanel.tsx:143`, pass the search:

```tsx
          <MatchCard key={match.studentId} match={match} search={search} onSendRequest={() => { setNotice(undefined); setSendingTo(match) }} />
```

- [ ] **Step 7: Run all frontend checks**

Run: `cd frontend && npm test && npm run lint && npm run build`
Expected: all PASS. The existing `FindBuddiesPage.test.tsx` must still pass unchanged; it never clicks the new button.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/features/matchexplanation frontend/src/features/matching/MatchCard.tsx frontend/src/features/matching/FindBuddiesPanel.tsx
git commit -m "feat: explain a match from its card on request"
```

---

### Task 10: Documentation, real-provider evidence and full verification

**Files:**
- Modify: `README.md` (libraries table), `docs/AI_USAGE.md`, `docs/REQUIREMENT_COVERAGE.md`, `docs/DESIGN_DECISIONS.md`

- [ ] **Step 1: Add Gemini to the README libraries table.** It is a service, not a Maven or npm dependency. Add after the `source-map-js` row:

```markdown
| Google Gemini API (free tier) | model set by `app.ai.explanation.model` | Optional AI wording of match explanations (E1); off without a key | Google [Gemini API terms](https://ai.google.dev/gemini-api/terms). Free-tier prompts may be used by Google to improve its products, so only anonymous `ExplanationEvidence` is sent |
```

Open the terms page yourself and confirm the data-use sentence is still accurate before committing.

- [ ] **Step 2: Run the app with your real key and record evidence.** With `application-local.yml` set up as in "Before you start":

```bash
cd backend && ./mvnw spring-boot:run
```

In another terminal:

```bash
cd frontend && npm run dev
```

Sign in as a seeded student, search by a course, and click **Explain this match** on:
1. a STRONG match,
2. a GOOD match,
3. a FAIR match.

Copy each explanation and that card's score breakdown. Check each sentence against the breakdown, and note any mismatch. Click the same card again and confirm the label still says AI-generated. That is the cache hit, and the backend log shows no new Gemini call. Then stop the backend, set `app.ai.explanation.timeout: 1ms` in `application-local.yml`, restart, click once, and confirm "Standard explanation" appears. Revert the timeout.

- [ ] **Step 3: Record it in `docs/AI_USAGE.md`.** Add a dated section "E1 AI match explanation (15 Oct 2026)" containing:
  - the model ID and that it is Gemini's free tier;
  - the full text of `prompts/match-explanation-v1.txt`;
  - the exact `ExplanationEvidence` fields sent, and the list of fields never sent;
  - the three real examples from Step 2, each with its breakdown and your accuracy note;
  - limitations: free-tier data use, an in-memory quota that resets on restart, an English-only prompt, and wording that can vary between models;
  - how this feature was built with AI assistance. Follow the file's existing short, dated bullet format.

- [ ] **Step 4: Add the E1 section to `docs/REQUIREMENT_COVERAGE.md`**, after the Team C sections:

```markdown
## AI match explanation (E1, #15)

| Requirement | Backend | Screen | Tests |
| --- | --- | --- | --- |
| Explain an existing match without changing scores or ranking | `MatchService.evaluate`, `MatchExplanationService` | `matchexplanation/MatchExplanationPanel` on `MatchCard` | `MatchServiceTest`, `MatchExplanationHttpTest` |
| Send only an allowlisted summary; no names, contacts, emails, IDs, messages or schedules | `ExplanationEvidence`, `ExplanationEvidenceFactory`, `ExplanationPromptBuilder` | — | `ExplanationEvidenceFactoryTest`, `ExplanationPromptBuilderTest`, `GeminiExplanationProviderTest` |
| Ground wording in evidence; no probability claims | Prompt v1, `ExplanationOutputValidator` | AI label states scores are separate | `ExplanationOutputValidatorTest`; real examples in `AI_USAGE.md` |
| Fall back on failure, timeout or unusable output; matching stays available | `GeminiExplanationProvider`, `ExplanationQuota`, `MatchExplanationService` | "Standard explanation" label; error with retry | `GeminiExplanationProviderTest`, `MatchExplanationServiceTest`, `ExplanationQuotaTest` |
| Cache repeated inputs; invalidate on preference, score, config, prompt or model change | `ExplanationHasher`, `ExplanationCache`, `match_explanations` | — | `ExplanationHasherTest`, `MatchExplanationHttpTest` |
| Authorisation and safe rendering | Same rules as search | Text rendered as plain text | `MatchExplanationHttpTest`, `MatchExplanationPanel.test.tsx` |
```

- [ ] **Step 5: Add a decision entry to `docs/DESIGN_DECISIONS.md`.** Match the file's existing heading style and record three decisions:
  - explanations are generated on click only, because of free-tier rate limits, search speed and data minimisation;
  - the cache is keyed by an evidence hash, so invalidation is automatic and the table stores no student IDs;
  - every failure returns the matching engine's own text labelled "Standard explanation", so E1 can never break matching.

- [ ] **Step 6: Run the full verification**

```bash
scripts/test-backend.sh
```

Expected: every backend test passes, zero failures. Then:

```bash
cd frontend && npm test && npm run lint && npm run build
```

Expected: all pass.

- [ ] **Step 7: Check that no secret is tracked**

```bash
git status --short && git grep -n "api-key:" -- ':!docs' ':!README.md'
```

Expected: `application-local.yml` is not listed, and the grep shows no `api-key:` line with a value in a tracked file.

- [ ] **Step 8: Commit**

```bash
git add README.md docs/AI_USAGE.md docs/REQUIREMENT_COVERAGE.md docs/DESIGN_DECISIONS.md
git commit -m "docs: record e1 ai match explanation coverage, decisions and examples"
```

Do not push. Opening the PR is a separate decision for Charlize. The PR description must flag the shared and Team A files: `MatchService.java`, `MatchCard.tsx`, `FindBuddiesPanel.tsx`, `application.yml`, `PostgresHttpTest.java` and `DatabaseBoundaryTest.java`.
