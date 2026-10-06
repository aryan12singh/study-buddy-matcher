package com.studybuddy.notification;

public enum NotificationFilter {
    ALL, REQUESTS, GROUPS;

    public boolean matches(Notification notification) {
        boolean group = notification.getType().name().startsWith("GROUP_");
        return this == ALL || (this == GROUPS && group) || (this == REQUESTS && !group);
    }
}
