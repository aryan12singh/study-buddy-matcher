package com.studybuddy.notification;

import java.time.Instant;

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
