package com.studybuddy.connection;

import com.studybuddy.common.error.ForbiddenActionException;

/**
 * Thrown when a student tries to act on a connection they are not part of,
 * such as ending someone else's connection.
 */
public class NotConnectionParticipantException extends ForbiddenActionException {

    public NotConnectionParticipantException(Long connectionId) {
        super("Only the two students in connection " + connectionId + " can do this");
    }
}
