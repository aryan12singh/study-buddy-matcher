package com.studybuddy.studygroup;

import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;

/**
 * Optional browse filters for study groups. A null field means "any".
 * Filtering happens in Java over the open groups rather than in a query with
 * nullable parameters, which keeps each rule readable and unit-testable.
 */
public record StudyGroupFilter(Long courseId, StudyGoal studyGoal, StudyMode studyMode) {

    public static StudyGroupFilter none() {
        return new StudyGroupFilter(null, null, null);
    }

    public boolean matches(StudyGroup group) {
        return matchesCourse(group) && matchesStudyGoal(group) && matchesStudyMode(group);
    }

    private boolean matchesCourse(StudyGroup group) {
        return courseId == null || courseId.equals(group.getCourse().getId());
    }

    private boolean matchesStudyGoal(StudyGroup group) {
        return studyGoal == null || group.getStudyGoals().contains(studyGoal);
    }

    /** EITHER on the filter or on the group is compatible with any mode. */
    private boolean matchesStudyMode(StudyGroup group) {
        StudyMode groupMode = group.getPreferredStudyMode();
        return studyMode == null || studyMode == StudyMode.EITHER
                || groupMode == StudyMode.EITHER || studyMode == groupMode;
    }
}
