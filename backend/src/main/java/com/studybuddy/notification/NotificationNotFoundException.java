package com.studybuddy.notification;

import com.studybuddy.common.error.NotFoundException;

public class NotificationNotFoundException extends NotFoundException {

    public NotificationNotFoundException(Long notificationId) {
        super("Notification " + notificationId + " not found");
    }
}
