package com.studybuddy.connection;

import com.studybuddy.common.error.NotFoundException;

/** Thrown when a connection id does not exist. Maps to 404 Not Found. */
public class ConnectionNotFoundException extends NotFoundException {

    public ConnectionNotFoundException(Long connectionId) {
        super("Connection " + connectionId + " not found");
    }
}
