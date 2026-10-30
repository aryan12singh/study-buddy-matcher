package com.studybuddy.studyroom;

import java.time.Instant;

/**
 * A student currently in the room: their presence, when their lease runs out, and whether they are
 * the leader, host or co-host.
 */
public record RoomParticipantDto(Long studentId, String name, PresenceState presence,
    Instant expiresAt, boolean leader, boolean host, boolean coHost) {}
