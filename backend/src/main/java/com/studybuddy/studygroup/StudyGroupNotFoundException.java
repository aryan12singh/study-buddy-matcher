package com.studybuddy.studygroup;

import com.studybuddy.common.error.NotFoundException;

/** Thrown when a study group id does not exist. Maps to 404 Not Found. */
public class StudyGroupNotFoundException extends NotFoundException {

    public StudyGroupNotFoundException(Long groupId) {
        super("Study group " + groupId + " not found");
    }
}
