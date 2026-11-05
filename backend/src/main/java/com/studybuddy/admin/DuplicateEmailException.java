package com.studybuddy.admin;

import com.studybuddy.common.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an account is created or edited with an email another account already uses. Maps to
 * 409 Conflict with code {@code DUPLICATE_EMAIL}.
 */
public class DuplicateEmailException extends ApiException {

    public DuplicateEmailException() {
        super(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "This email is already registered");
    }
}
