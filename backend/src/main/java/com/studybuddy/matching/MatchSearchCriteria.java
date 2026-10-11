package com.studybuddy.matching;

import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
/**
 * What a student searched for. Every field is an optional filter and may be
 * null; a search with none set ranks every other active student.
 */
public record MatchSearchCriteria(
        Long courseId,
        StudyGoal studyGoal,
        StudyMode studyMode,
        GroupSizePreference groupSize,
        Integer minSharedHours,
        MatchQuality minQuality) {
}
