package com.studybuddy.connection;

public class ConnectionNotFoundException extends RuntimeException {

    public ConnectionNotFoundException(Long connectionId) {
        super("Connection " + connectionId + " not found");
    }
}
