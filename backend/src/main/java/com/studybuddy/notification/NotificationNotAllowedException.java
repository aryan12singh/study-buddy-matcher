package com.studybuddy.notification;

import com.studybuddy.common.error.ForbiddenActionException;

/** Thrown when a student tries to act on a notification addressed to someone else. */
public class NotificationNotAllowedException extends ForbiddenActionException {

    public NotificationNotAllowedException(Long notificationId) {
        super("Notification " + notificationId + " belongs to another student");
    }
}
