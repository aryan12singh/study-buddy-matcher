package com.studybuddy.studygroup;

import com.studybuddy.common.error.InvalidInputException;

public class InvalidStudyGroupException extends InvalidInputException {

    public InvalidStudyGroupException(String message) {
        super(message);
    }

    public InvalidStudyGroupException(String field, String message) {
        super(field, message);
    }
}
