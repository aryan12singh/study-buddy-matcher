package com.studybuddy.connection;

/**
 * Thrown when a student tries to act on a connection they are not part of,
 * such as ending someone else's connection.
 */
public class NotConnectionParticipantException extends RuntimeException {

    public NotConnectionParticipantException(Long connectionId) {
        super("Only the two students in connection " + connectionId + " can do this");
    }
}
