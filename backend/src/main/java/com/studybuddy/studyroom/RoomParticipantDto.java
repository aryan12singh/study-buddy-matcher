package com.studybuddy.studyroom;

import java.time.Instant;

public record RoomParticipantDto(Long studentId, String name, PresenceState presence,
    Instant expiresAt, boolean leader, boolean host, boolean coHost) {}
