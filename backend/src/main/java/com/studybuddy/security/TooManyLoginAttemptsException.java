package com.studybuddy.security;

import com.studybuddy.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class TooManyLoginAttemptsException extends ApiException {

    public TooManyLoginAttemptsException(long minutesToWait) {
        super(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_LOGIN_ATTEMPTS", "Too many incorrect sign-in attempts. Try again in "
            + minutesToWait + (minutesToWait == 1 ? " minute." : " minutes."));
    }
}
