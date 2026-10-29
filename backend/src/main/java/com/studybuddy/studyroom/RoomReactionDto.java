package com.studybuddy.studyroom;

import java.time.Instant;

/** A recent reaction as every room member sees it; the id lets a browser animate it only once. */
public record RoomReactionDto(Long id, RoomReactionType type, Long studentId, String senderName, Instant sentAt) {}
