package com.studybuddy.matching;

import com.studybuddy.matching.strategy.MatchingStrategyType;
import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;

import java.util.Set;

/**
 * One candidate scored exactly as search scores them, plus the effective search intent and course provenance,
 * for features that explain a match. Holds no entities, so nothing private can leak from it.
 */
public record MatchEvaluation(MatchingStrategyType strategy, MatchResultDto result, StudyMode searcherStudyMode,
        Set<StudyGoal> effectiveStudyGoals, StudyGoal searchedGoal, GroupSizePreference searcherGroupSize,
        GroupSizePreference candidateGroupSize, RelevantCourseDto scoringCourse, RelevantCourseDto sharedCourse) {

    public MatchEvaluation {
        effectiveStudyGoals = Set.copyOf(effectiveStudyGoals);
    }
}
