package com.studybuddy.studyplan;

import com.studybuddy.common.error.NotFoundException;

/** Thrown when a study plan entry does not exist in the given plan. Maps to 404 Not Found. */
public class StudyPlanEntryNotFoundException extends NotFoundException {
    public StudyPlanEntryNotFoundException(Long planId, Long entryId) {
        super("Entry " + entryId + " not found in study plan " + planId);
    }
}
