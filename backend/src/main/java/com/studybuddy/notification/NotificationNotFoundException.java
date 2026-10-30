package com.studybuddy.notification;

import com.studybuddy.common.error.NotFoundException;

/** Thrown when a notification id does not exist. Maps to 404 Not Found. */
public class NotificationNotFoundException extends NotFoundException {

    public NotificationNotFoundException(Long notificationId) {
        super("Notification " + notificationId + " not found");
    }
}
