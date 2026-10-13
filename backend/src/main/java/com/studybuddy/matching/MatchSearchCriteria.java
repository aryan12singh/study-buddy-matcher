package com.studybuddy.matching;

import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;

/** A course or study goal is required. Other values narrow the current search only. */
public record MatchSearchCriteria(
        Long courseId, StudyGoal studyGoal, StudyMode studyMode, GroupSizePreference groupSize,
        Integer minSharedHours, MatchQuality minQuality, MatchSort sort, Integer limit) {

    public MatchSearchCriteria(Long courseId, StudyGoal studyGoal, StudyMode studyMode,
            GroupSizePreference groupSize, Integer minSharedHours, MatchQuality minQuality) {
        this(courseId, studyGoal, studyMode, groupSize, minSharedHours, minQuality, null, null);
    }
}
