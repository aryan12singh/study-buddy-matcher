package com.studybuddy.admin;

/** Thrown when an admin action conflicts with the rules, such as deactivating your own account. */
public class AdminActionNotAllowedException extends RuntimeException {

    public AdminActionNotAllowedException(String message) {
        super(message);
    }
}
