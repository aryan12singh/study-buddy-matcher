package com.studybuddy.common.error;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a signed-in user is not allowed to perform an action, such as a student calling an
 * administrator endpoint. Maps to 403 Forbidden.
 */
public class ForbiddenActionException extends ApiException {

    public ForbiddenActionException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
