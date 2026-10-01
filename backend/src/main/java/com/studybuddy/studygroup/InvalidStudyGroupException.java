package com.studybuddy.studygroup;

/**
 * Thrown when the details supplied for a group are invalid, such as an
 * availability slot that ends before it starts.
 */
public class InvalidStudyGroupException extends RuntimeException {

    public InvalidStudyGroupException(String message) {
        super(message);
    }
}
