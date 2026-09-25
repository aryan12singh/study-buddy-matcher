package com.studybuddy.matchrequest;

public class MatchRequestNotFoundException extends RuntimeException {

    public MatchRequestNotFoundException(Long requestId) {
        super("Match request " + requestId + " not found");
    }
}
