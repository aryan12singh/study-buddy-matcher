package com.studybuddy.studygroup;

import java.time.Instant;

/** A join request as the API sees it. Names and ids only, no contact numbers. */
public record GroupJoinRequestDto(
        Long id,
        Long groupId,
        String groupName,
        Long studentId,
        String studentName,
        String message,
        GroupJoinRequestStatus status,
        Instant createdAt,
        Instant respondedAt,
        boolean groupActive) {
}
