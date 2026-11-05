package com.studybuddy.studyroom;

import java.time.Instant;

/**
 * A student lost access to every room at once, by deactivation. Unlike
 * {@link RoomVisitEnded}, it does not depend on a live tab: a visit can outlive its last lease.
 */
public record StudentRoomAccessRevoked(Long studentId, Instant at) {}
