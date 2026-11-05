package com.studybuddy.user;

import com.studybuddy.common.error.NotFoundException;

/** Thrown when an account id does not exist. Maps to 404 Not Found. */
public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(Long userId) {
        super("User " + userId + " not found");
    }
}
