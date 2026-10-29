package com.studybuddy.studyroom;

import java.time.Instant;

/** A student left a room from their last open tab. Expired tabs end visits lazily instead. */
public record RoomVisitEnded(Long studentId, Long groupId, Instant at) {}
