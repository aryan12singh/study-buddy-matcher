package com.studybuddy.matchrequest;

import java.time.LocalDateTime;

/**
 * A match request as the API sees it. Carries names and ids only; contact
 * numbers are deliberately absent because a request exists before any
 * connection does.
 */
public record MatchRequestDto(
        Long id,
        Long senderId,
        String senderName,
        Long receiverId,
        String receiverName,
        String message,
        MatchRequestStatus status,
        LocalDateTime createdAt) {
}
