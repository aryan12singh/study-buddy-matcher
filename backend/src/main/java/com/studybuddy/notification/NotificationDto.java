package com.studybuddy.notification;

import java.time.LocalDateTime;

/** A notification as the API sees it. The recipient is implied: it is always the caller. */
public record NotificationDto(
        Long id,
        NotificationType type,
        String message,
        boolean read,
        LocalDateTime createdAt) {
}
