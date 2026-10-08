package com.studybuddy.user;

import com.studybuddy.common.error.NotFoundException;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(Long userId) {
        super("User " + userId + " not found");
    }
}
