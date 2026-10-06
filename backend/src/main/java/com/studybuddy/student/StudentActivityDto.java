package com.studybuddy.student;

public record StudentActivityDto(
        long activeConnections,
        long pendingIncoming,
        long pendingOutgoing,
        long acceptedGroups) {
}
