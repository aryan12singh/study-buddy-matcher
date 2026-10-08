package com.studybuddy.matchrequest;

import com.studybuddy.common.error.NotFoundException;

public class MatchRequestNotFoundException extends NotFoundException {

    public MatchRequestNotFoundException(Long requestId) {
        super("Match request " + requestId + " not found");
    }
}
