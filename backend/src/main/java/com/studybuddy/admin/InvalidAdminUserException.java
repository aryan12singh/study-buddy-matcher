package com.studybuddy.admin;

import com.studybuddy.common.error.InvalidInputException;

/** Thrown when the account details an admin supplies are missing or invalid. */
public class InvalidAdminUserException extends InvalidInputException {

    public InvalidAdminUserException(String message) {
        super(message);
    }
}
