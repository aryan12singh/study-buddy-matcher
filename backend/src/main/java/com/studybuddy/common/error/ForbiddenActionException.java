package com.studybuddy.common.error;

import org.springframework.http.HttpStatus;

public class ForbiddenActionException extends ApiException {

    public ForbiddenActionException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
