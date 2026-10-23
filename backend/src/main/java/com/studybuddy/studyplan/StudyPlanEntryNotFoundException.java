package com.studybuddy.studyplan;

import com.studybuddy.common.error.NotFoundException;

public class StudyPlanEntryNotFoundException extends NotFoundException {
    public StudyPlanEntryNotFoundException(Long planId, Long entryId) {
        super("Entry " + entryId + " not found in study plan " + planId);
    }
}
