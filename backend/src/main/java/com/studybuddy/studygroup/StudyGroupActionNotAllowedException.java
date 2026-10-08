package com.studybuddy.studygroup;

import com.studybuddy.common.error.ConflictException;

/**
 * Thrown when an action conflicts with the group's current state, such as
 * joining a closed or full group, or asking to join twice.
 */
public class StudyGroupActionNotAllowedException extends ConflictException {

    public StudyGroupActionNotAllowedException(String message) {
        super(message);
    }
}
