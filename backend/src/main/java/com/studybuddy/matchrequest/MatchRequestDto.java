package com.studybuddy.matchrequest;

import java.time.Instant;

/** A privacy-safe request history item, with its structured search context. */
public record MatchRequestDto(
        Long id,
        Long senderId,
        String senderName,
        Long receiverId,
        String receiverName,
        String message,
        MatchRequestStatus status,
        Instant createdAt,
        Instant respondedAt,
        MatchRequestContextDto context) {
}
