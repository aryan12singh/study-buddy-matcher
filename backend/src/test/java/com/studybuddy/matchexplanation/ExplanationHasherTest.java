package com.studybuddy.matchexplanation;

import com.studybuddy.matching.MatchEvaluation;
import com.studybuddy.student.StudyGoal;

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
    @Test
    void exactCardIdentityDoesNotPreventEquivalentWordingFromBeingReused() {
        assertEquals(hasher.hash(factory.from(ExplanationFixtures.evaluation(0.7234, "a".repeat(64))), "v3", "m"),
                hasher.hash(factory.from(ExplanationFixtures.evaluation(0.7189, "b".repeat(64))), "v3", "m"));
    }

    @Test
    void searchedGoalRemainsPartOfSemanticIdentityEvenWhenSharedGoalsAreTheSame() {
        MatchEvaluation saved = ExplanationFixtures.evaluation();
        MatchEvaluation searched = new MatchEvaluation(saved.strategy(), saved.result(), saved.searcherStudyMode(),
                saved.effectiveStudyGoals(), StudyGoal.EXAM_PREPARATION, saved.searcherGroupSize(), saved.candidateGroupSize(),
                saved.scoringCourse(), saved.sharedCourse());

        assertEquals(factory.from(saved).sharedGoals(), factory.from(searched).sharedGoals());
        assertNotEquals(hasher.hash(factory.from(saved), "v3", "m"), hasher.hash(factory.from(searched), "v3", "m"));
    }
}
