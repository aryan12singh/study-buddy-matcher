package com.studybuddy.matchexplanation;

import com.studybuddy.matching.CriterionScoreDto;
import com.studybuddy.matching.MatchEvaluation;
import com.studybuddy.matching.MatchResultDto;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.student.StudyGoal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    void keepsTheSharedFlagSoAnUnsharedPreferenceIsNotWordedAsAMismatch() {
        MatchEvaluation evaluation = ExplanationFixtures.evaluation();
        MatchResultDto result = evaluation.result();
        List<CriterionScoreDto> breakdown = result.breakdown().stream()
                .map(score -> score.criterion() == MatchingCriterion.STUDY_MODE
                        ? new CriterionScoreDto(score.criterion(), 0.0, true, false) : score)
                .toList();
        MatchEvaluation unshared = new MatchEvaluation(evaluation.strategy(),
                new MatchResultDto(result.studentId(), result.name(), result.school(), result.programme(),
                        result.yearOfStudy(), result.studyMode(), result.studyGoals(), result.quality(),
                        result.totalScore(), result.explanation(), result.relevantCourse(), breakdown,
                        result.sharedHoursPerWeek(), result.sharedSlots(), result.relationship()),
                evaluation.searcherStudyMode(), evaluation.searcherStudyGoals(), evaluation.searcherGroupSize(),
                evaluation.candidateGroupSize());

        List<CriterionEvidence> criteria = factory.from(unshared).criteria();

        assertFalse(criteria.stream().filter(c -> c.criterion() == MatchingCriterion.STUDY_MODE).findFirst().orElseThrow().known());
        assertTrue(criteria.stream().filter(c -> c.criterion() == MatchingCriterion.COURSE).findFirst().orElseThrow().known());
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
        MatchEvaluation evaluation = ExplanationFixtures.evaluation();
        MatchResultDto result = evaluation.result();
        MatchEvaluation withoutCourse = new MatchEvaluation(evaluation.strategy(),
                new MatchResultDto(result.studentId(), result.name(), result.school(), result.programme(),
                        result.yearOfStudy(), result.studyMode(), result.studyGoals(), result.quality(),
                        result.totalScore(), result.explanation(), null, result.breakdown(), result.sharedHoursPerWeek(),
                        result.sharedSlots(), result.relationship()),
                evaluation.searcherStudyMode(), evaluation.searcherStudyGoals(), evaluation.searcherGroupSize(),
                evaluation.candidateGroupSize());

        assertNull(factory.from(withoutCourse).course());
    }
}
