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
                .map(score -> new CriterionEvidence(score.criterion(), round(score.score(), SCORE_PRECISION), score.enabled(),
                        score.known()))
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
