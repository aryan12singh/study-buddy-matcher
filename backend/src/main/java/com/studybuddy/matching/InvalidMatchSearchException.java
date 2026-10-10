package com.studybuddy.matching;

import com.studybuddy.common.error.InvalidInputException;
/** Thrown when a match search is missing its course or goal, or has an invalid filter. */
public class InvalidMatchSearchException extends InvalidInputException {

    public InvalidMatchSearchException(String message) {
        super(message);
    }
}
