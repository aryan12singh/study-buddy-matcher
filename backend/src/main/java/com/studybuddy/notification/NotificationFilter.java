package com.studybuddy.notification;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** The notification tabs: everything, buddy requests and connections, or study groups. */
public enum NotificationFilter {
    ALL, REQUESTS, GROUPS;

    /** The notification types this tab shows, so the database filters them rather than Java. */
    public Set<NotificationType> types() {
        return Arrays.stream(NotificationType.values()).filter(this::includes).collect(Collectors.toUnmodifiableSet());
    }

    private boolean includes(NotificationType type) {
        boolean group = type.name().startsWith("GROUP_");
        return this == ALL || (this == GROUPS && group) || (this == REQUESTS && !group);
    }
}
