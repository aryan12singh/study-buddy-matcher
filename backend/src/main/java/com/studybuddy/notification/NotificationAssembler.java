package com.studybuddy.notification;

import org.springframework.stereotype.Component;

/**
 * Converts {@link Notification} entities into {@link NotificationDto}s so the
 * entity never leaves the service layer.
 */
@Component
public class NotificationAssembler {

    public NotificationDto toDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
