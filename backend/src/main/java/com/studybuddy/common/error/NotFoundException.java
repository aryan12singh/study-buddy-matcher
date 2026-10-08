package com.studybuddy.common.error;

import org.springframework.http.HttpStatus;

/** Base for a requested resource that does not exist or is not visible to the caller. */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
