package com.studybuddy.studygroup;

/**
 * Thrown when an action conflicts with the group's current state, such as
 * joining a closed or full group, or asking to join twice.
 */
public class StudyGroupActionNotAllowedException extends RuntimeException {

    public StudyGroupActionNotAllowedException(String message) {
        super(message);
    }
}
