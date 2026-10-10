package com.studybuddy.matching;

import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
/**
 * What a student searched for. A search starts from a course or a study goal;
 * every other field is an optional filter and may be null.
 */
public record MatchSearchCriteria(
        Long courseId,
        StudyGoal studyGoal,
        StudyMode studyMode,
        GroupSizePreference groupSize,
        Integer minSharedHours,
        MatchQuality minQuality) {
}
