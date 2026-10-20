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
        List<CriterionEvidence> criteria, double sharedHoursPerWeek, CourseEvidence scoringCourse, CourseEvidence sharedCourse,
        StudyGoal searchedGoal, List<StudyGoal> sharedGoals,
        StudyMode searcherStudyMode, StudyMode candidateStudyMode, GroupSizePreference searcherGroupSize,
        GroupSizePreference candidateGroupSize) {

    public ExplanationEvidence {
        criteria = List.copyOf(criteria);
        sharedGoals = List.copyOf(sharedGoals);
    }
}
