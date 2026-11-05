package com.studybuddy.student;

/**
 * Counts for a student's home screen: active connections, pending requests in each direction, and
 * open groups they belong to, including ones they lead.
 */
public record StudentActivityDto(
        long activeConnections,
        long pendingIncoming,
        long pendingOutgoing,
        long acceptedGroups) {
}
