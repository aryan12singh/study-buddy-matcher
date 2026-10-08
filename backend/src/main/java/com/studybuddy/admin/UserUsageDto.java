package com.studybuddy.admin;

/**
 * A student's basic usage, as the admin screen shows it. What counts here is
 * decided in {@link UserUsageCounter}.
 */
public record UserUsageDto(
        long activeConnections,
        long matchRequestsSent,
        long groupsLed,
        long groupsJoined,
        long acceptedGroups) {
}
