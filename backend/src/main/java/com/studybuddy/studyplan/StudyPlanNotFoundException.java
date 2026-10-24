package com.studybuddy.studyplan;

import com.studybuddy.common.error.NotFoundException;

/** Also used for a plan the student may not see, so a plan's ID reveals nothing about it. */
public class StudyPlanNotFoundException extends NotFoundException {
    public StudyPlanNotFoundException(Long planId) {
        super("Study plan " + planId + " not found");
    }
}
