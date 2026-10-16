package com.studybuddy.studysession;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record SessionDto(Long id, long version, SessionScope scope, Long groupId, Long connectionId,
    String title, LocalDateTime startLocal, LocalDateTime endLocal, Instant startAt, Instant endAt,
    String timeZone, int occurrenceCount, String agenda, String location, String roomLink,
    SessionStatus status, boolean canManage, List<SessionOccurrenceDto> occurrences,
    SessionOccurrenceDto nextOccurrence, Instant updatedAt, Instant cancelledAt) { }
