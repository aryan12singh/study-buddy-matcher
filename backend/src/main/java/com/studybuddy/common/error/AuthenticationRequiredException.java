package com.studybuddy.common.error;

import org.springframework.http.HttpStatus;

public class AuthenticationRequiredException extends ApiException {

    public AuthenticationRequiredException() {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Sign in with an active account to continue");
    }
}
