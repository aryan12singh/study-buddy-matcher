package com.studybuddy.studysession;

import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Builds a {@link SessionDto}: the schedule with every weekly occurrence, the next one still to
 * come, and whether the viewer may manage the session (any participant of a buddy session, only the
 * leader of a group session).
 */
@Component
public class SessionAssembler {
    public SessionDto toDto(StudySession session, Long actorId, Instant now) {
        var occurrences = session.schedule().occurrences();
        var next = session.getStatus() == SessionStatus.CANCELLED ? null
            : occurrences.stream().filter(slot -> slot.endAt().isAfter(now)).findFirst().orElse(null);
        boolean group = session.getGroup() != null;
        return new SessionDto(session.getId(), session.getVersion(), group ? SessionScope.GROUP : SessionScope.BUDDY,
            group ? session.getGroup().getId() : null, group ? null : session.getConnection().getId(),
            session.getTitle(), session.getStartLocal(), session.getEndLocal(), session.getStartAt(), session.getEndAt(),
            session.getTimeZone(), session.getOccurrenceCount(), session.getAgenda(), session.getLocation(), session.getRoomLink(),
            session.getStatus(), !group || session.getGroup().isLeader(actorId), occurrences, next,
            session.getUpdatedAt(), session.getCancelledAt());
    }
}
