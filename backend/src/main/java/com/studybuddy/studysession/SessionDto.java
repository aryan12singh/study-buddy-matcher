package com.studybuddy.studysession;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A study session as a participant sees it: what, when (local wall time and instants), where, every
 * weekly occurrence and the next one, its status and version, and whether the viewer may manage it.
 */
public record SessionDto(Long id, long version, SessionScope scope, Long groupId, Long connectionId,
    String title, LocalDateTime startLocal, LocalDateTime endLocal, Instant startAt, Instant endAt,
    String timeZone, int occurrenceCount, String agenda, String location, String roomLink,
    SessionStatus status, boolean canManage, List<SessionOccurrenceDto> occurrences,
    SessionOccurrenceDto nextOccurrence, Instant updatedAt, Instant cancelledAt) { }
