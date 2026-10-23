package com.studybuddy.studyroom;

import java.time.Instant;

/** A group's shared timer was started from idle; published inside the start's transaction. */
public record RoomStarted(Long roomId, Long actorId, Instant startedAt) {
}
