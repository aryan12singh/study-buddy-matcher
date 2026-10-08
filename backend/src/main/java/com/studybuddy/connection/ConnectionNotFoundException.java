package com.studybuddy.connection;

import com.studybuddy.common.error.NotFoundException;

public class ConnectionNotFoundException extends NotFoundException {

    public ConnectionNotFoundException(Long connectionId) {
        super("Connection " + connectionId + " not found");
    }
}
