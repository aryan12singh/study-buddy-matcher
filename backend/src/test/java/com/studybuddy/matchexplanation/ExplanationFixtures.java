package com.studybuddy.matchexplanation;

import com.studybuddy.matching.CriterionScoreDto;
import com.studybuddy.matching.MatchEvaluation;
import com.studybuddy.matching.MatchQuality;
import com.studybuddy.matching.MatchResultDto;
import com.studybuddy.matching.RelevantCourseDto;
import com.studybuddy.matching.SharedSlotDto;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.scoring.MatchingWeights;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import com.studybuddy.profile.RelationshipState;
import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import tools.jackson.databind.json.JsonMapper;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A fully populated match evaluation, including every private field the evidence must drop. */
final class ExplanationFixtures {
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final long CANDIDATE_ID = 424242L;
    static final String EVALUATION_ID = "a".repeat(64);
    /** Group size is disabled in the fixture breakdown, so its weight is zero. */
    static final MatchingWeights WEIGHTS = new MatchingWeights(Map.of(MatchingCriterion.COURSE, 0.3,
            MatchingCriterion.AVAILABILITY, 0.3, MatchingCriterion.STUDY_MODE, 0.2, MatchingCriterion.STUDY_GOAL, 0.2,
            MatchingCriterion.GROUP_SIZE, 0.0));

    private ExplanationFixtures() {
    }

    static MatchEvaluation evaluation() {
        return evaluation(0.7234);
    }

    static MatchEvaluation evaluation(double totalScore) {
        return evaluation(totalScore, EVALUATION_ID);
    }

    static MatchEvaluation evaluation(double totalScore, String evaluationId) {
        MatchResultDto result = new MatchResultDto(CANDIDATE_ID, "Jamie Lee", "SCIS", "Information Systems", 2,
                StudyMode.EITHER, List.of(StudyGoal.EXAM_PREPARATION, StudyGoal.PROBLEM_SOLVING), MatchQuality.GOOD,
                totalScore, "good match: same course", evaluationId, new RelevantCourseDto(4L, "IS442", "Object Oriented Programming"),
                List.of(new CriterionScoreDto(MatchingCriterion.COURSE, 1.0, true, true),
                        new CriterionScoreDto(MatchingCriterion.AVAILABILITY, 0.3333, true, true),
                        new CriterionScoreDto(MatchingCriterion.STUDY_MODE, 1.0, true, true),
                        new CriterionScoreDto(MatchingCriterion.STUDY_GOAL, 0.5, true, true),
                        new CriterionScoreDto(MatchingCriterion.GROUP_SIZE, 0.5, false, true)),
                3.4, List.of(new SharedSlotDto(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0))),
                RelationshipState.STRANGER);
        return new MatchEvaluation(MatchingStrategyType.BALANCED, result, StudyMode.ONLINE,
                Set.of(StudyGoal.EXAM_PREPARATION, StudyGoal.CONCEPT_REVIEW), null, GroupSizePreference.ONE_TO_ONE,
                GroupSizePreference.EITHER, result.relevantCourse(), result.relevantCourse(), WEIGHTS);
    }
}
