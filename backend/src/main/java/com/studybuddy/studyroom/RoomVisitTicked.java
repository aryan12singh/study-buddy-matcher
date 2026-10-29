package com.studybuddy.studyroom;

import java.time.Instant;

/**
 * A student's lease was taken or renewed. Carries the room's timer as it stood, so listeners can
 * work out how much of the time since the student's last tick was focus time.
 */
public record RoomVisitTicked(Long studentId, Long groupId, String groupName, PomodoroTimer timer, Instant at) {}
