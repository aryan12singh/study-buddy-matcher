package com.studybuddy.matchrequest;

/**
 * Thrown when a student tries a match request action the rules forbid, such
 * as requesting themselves or responding to a request sent to someone else.
 */
public class MatchRequestNotAllowedException extends RuntimeException {

    public MatchRequestNotAllowedException(String message) {
        super(message);
    }
}
