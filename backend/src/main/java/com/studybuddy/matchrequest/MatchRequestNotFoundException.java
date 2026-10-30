package com.studybuddy.matchrequest;

import com.studybuddy.common.error.NotFoundException;

/** Thrown when a match request id does not exist. Maps to 404 Not Found. */
public class MatchRequestNotFoundException extends NotFoundException {

    public MatchRequestNotFoundException(Long requestId) {
        super("Match request " + requestId + " not found");
    }
}
