package com.studybuddy.notification;

import java.time.Instant;

/**
 * One notification as its recipient sees it: the message, whether it is read, and the resource it
 * links to, so the screen can open the right page.
 */
public record NotificationDto(
        Long id,
        NotificationType type,
        String message,
        boolean read,
        Instant createdAt,
        NotificationResourceType resourceType,
        Long resourceId,
        String eventKey,
        NotificationRequestDirection requestDirection) {
}
