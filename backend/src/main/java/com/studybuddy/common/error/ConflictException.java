package com.studybuddy.common.error;

import org.springframework.http.HttpStatus;

/** Base for an action that conflicts with the current state, such as a repeated decision. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "STATE_CONFLICT", message);
    }
}
