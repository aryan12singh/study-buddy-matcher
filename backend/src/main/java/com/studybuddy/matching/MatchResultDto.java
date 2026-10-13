package com.studybuddy.matching;

import com.studybuddy.profile.RelationshipState;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;

import java.util.List;

/**
 * Privacy-safe display data and the immutable scoring result for one candidate.
 */
public record MatchResultDto(
        Long studentId,
        String name,
        String school,
        String programme,
        Integer yearOfStudy,
        StudyMode studyMode,
        List<StudyGoal> studyGoals,
        MatchQuality quality,
        double totalScore,
        String explanation,
        RelevantCourseDto relevantCourse,
        List<CriterionScoreDto> breakdown,
        double sharedHoursPerWeek,
        List<SharedSlotDto> sharedSlots,
        RelationshipState relationship) {
}
