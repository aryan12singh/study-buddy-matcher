package com.studybuddy.common.error;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request needs a signed-in, active account and has none. Maps to 401 Unauthorized.
 */
public class AuthenticationRequiredException extends ApiException {

    public AuthenticationRequiredException() {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Sign in with an active account to continue");
    }
}
