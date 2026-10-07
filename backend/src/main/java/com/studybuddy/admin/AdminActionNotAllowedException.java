package com.studybuddy.admin;

import com.studybuddy.common.error.ConflictException;

/** Thrown when an admin action conflicts with the rules, such as deactivating your own account. */
public class AdminActionNotAllowedException extends ConflictException {

    public AdminActionNotAllowedException(String message) {
        super(message);
    }
}
