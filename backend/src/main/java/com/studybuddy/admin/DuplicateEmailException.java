package com.studybuddy.admin;

import com.studybuddy.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends ApiException {

    public DuplicateEmailException() {
        super(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "This email is already registered");
    }
}
