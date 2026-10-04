package com.studybuddy.notification;

/** Thrown when a student tries to act on a notification addressed to someone else. */
public class NotificationNotAllowedException extends RuntimeException {

    public NotificationNotAllowedException(Long notificationId) {
        super("Notification " + notificationId + " belongs to another student");
    }
}
