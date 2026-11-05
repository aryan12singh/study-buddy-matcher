package com.studybuddy.studyroom;

import java.time.Instant;

/**
 * A group's room closed with the group. Everyone's visit to it ends, including visits whose tab
 * lease has already lapsed.
 */
public record StudyRoomClosed(Long groupId, Instant at) {}
