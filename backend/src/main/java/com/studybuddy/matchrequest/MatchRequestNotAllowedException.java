package com.studybuddy.matchrequest;

import com.studybuddy.common.error.ConflictException;

/**
 * Thrown when a match request conflicts with the current relationship, such as
 * requesting yourself, an existing buddy, or a student you already have a pending request with.
 */
public class MatchRequestNotAllowedException extends ConflictException {

    public MatchRequestNotAllowedException(String message) {
        super(message);
    }
}
