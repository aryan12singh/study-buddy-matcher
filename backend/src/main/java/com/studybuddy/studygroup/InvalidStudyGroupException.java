package com.studybuddy.studygroup;

import com.studybuddy.common.error.InvalidInputException;

/**
 * Thrown when study group details break a rule, such as a maximum size below the current member
 * count. Maps to 400 Bad Request, naming the field when there is one.
 */
public class InvalidStudyGroupException extends InvalidInputException {

    public InvalidStudyGroupException(String message) {
        super(message);
    }

    public InvalidStudyGroupException(String field, String message) {
        super(field, message);
    }
}
