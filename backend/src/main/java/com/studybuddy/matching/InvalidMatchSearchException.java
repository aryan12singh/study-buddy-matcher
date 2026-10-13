package com.studybuddy.matching;

import com.studybuddy.common.error.InvalidInputException;
/** Thrown when a match search has an invalid filter. */
public class InvalidMatchSearchException extends InvalidInputException {

    public InvalidMatchSearchException(String message) {
        super(message);
    }
}
