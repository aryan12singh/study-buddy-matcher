package com.studybuddy.studygroup;

/**
 * Thrown when a student who does not lead a group tries a leader-only action:
 * editing, closing, removing members or answering join requests.
 */
public class NotGroupLeaderException extends RuntimeException {

    public NotGroupLeaderException(Long groupId) {
        super("Only the leader of study group " + groupId + " can do this");
    }
}
