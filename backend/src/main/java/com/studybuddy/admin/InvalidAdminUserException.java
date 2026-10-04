package com.studybuddy.admin;

/** Thrown when the account details an admin supplies are missing or invalid. */
public class InvalidAdminUserException extends RuntimeException {

    public InvalidAdminUserException(String message) {
        super(message);
    }
}
