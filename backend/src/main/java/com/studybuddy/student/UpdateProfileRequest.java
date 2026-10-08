package com.studybuddy.student;

import java.util.List;
import java.util.Set;

/** The whole editable profile, replacing what is saved. Checked by {@link ProfileService}. */
public record UpdateProfileRequest(
        String name,
        String school,
        String programme,
        Integer yearOfStudy,
        String contactNumber,
        List<Long> courseIds,
        Long targetCourseId,
        StudyMode preferredStudyMode,
        GroupSizePreference groupSizePreference,
        Set<StudyGoal> studyGoals) {
}
