package com.studybuddy.connection;

import java.time.Instant;

/**
 * A connection as one of its two students sees it: who is on the other side
 * and since when. Deliberately carries no contact number; that is shown only
 * through the profile view, which checks the connection is still active.
 */
public record ConnectionDto(
        Long id,
        Long otherStudentId,
        String otherStudentName,
        Instant createdAt) {
}
