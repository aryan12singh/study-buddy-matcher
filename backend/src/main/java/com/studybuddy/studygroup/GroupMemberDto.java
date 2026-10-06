package com.studybuddy.studygroup;

import java.time.Instant;

/**
 * A member as other group members see them. No contact number: belonging to
 * the same group is not a connection.
 */
public record GroupMemberDto(
        Long studentId,
        String name,
        boolean leader,
        Instant joinedAt) {
}
