package com.studybuddy.matchrequest;

import com.studybuddy.common.error.ConflictException;

/**
 * Thrown when a student tries a match request action the rules forbid, such
 * as requesting themselves or responding to a request sent to someone else.
 */
public class MatchRequestNotAllowedException extends ConflictException {

    public MatchRequestNotAllowedException(String message) {
        super(message);
    }
}
