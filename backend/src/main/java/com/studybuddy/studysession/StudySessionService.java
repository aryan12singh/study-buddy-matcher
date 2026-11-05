package com.studybuddy.studysession;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.ConflictException;
import com.studybuddy.common.error.InvalidInputException;
import com.studybuddy.common.error.NotFoundException;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Study session use cases for groups and buddy connections: list, create, view, edit, cancel,
 * suggest shared times and export to a calendar. Only participants can see a session, and a session
 * they cannot see is reported as not found. Changes lock the group or connection and check the
 * session version, then notify the other participants.
 */
@Service
@Transactional
public class StudySessionService {
    private final StudySessionRepository sessions;
    private final SessionAccess access;
    private final AccountAccess accounts;
    private final SessionSettings settings;
    private final SessionAssembler assembler;
    private final SessionAvailability availability;
    private final SessionEvents events;
    private final SessionCalendar calendar;
    private final Clock clock;
    public StudySessionService(StudySessionRepository sessions, SessionAccess access, AccountAccess accounts,
        SessionSettings settings, SessionAssembler assembler, SessionAvailability availability, SessionEvents events,
        SessionCalendar calendar, Clock clock) {
        this.sessions = sessions;
        this.access = access;
        this.accounts = accounts;
        this.settings = settings;
        this.assembler = assembler;
        this.availability = availability;
        this.events = events;
        this.calendar = calendar;
        this.clock = clock;
    }
    /**
     * The student's scheduled sessions across their open groups and active connections, soonest
     * first. A session with no occurrence still to come is left out.
     */
    @Transactional(readOnly = true)
    public List<SessionDto> mine(Long actorId) {
        accounts.requireStudent(actorId);
        var now = clock.instant();
        return sessions.findMine(actorId).stream().map(session -> assembler.toDto(session, actorId, now))
            .filter(dto -> dto.nextOccurrence() != null).sorted(Comparator.comparing(dto -> dto.nextOccurrence().startAt())).toList();
    }
    @Transactional(readOnly = true)
    public List<SessionDto> groupSessions(Long groupId, Long actorId) {
        access.readGroup(groupId, actorId);
        return dtos(sessions.findByGroupIdOrderByStartAtAsc(groupId), actorId);
    }
    @Transactional(readOnly = true)
    public List<SessionDto> buddySessions(Long connectionId, Long actorId) {
        access.readConnection(connectionId, actorId);
        return dtos(sessions.findByConnectionIdOrderByStartAtAsc(connectionId), actorId);
    }
    public SessionDto createGroup(Long groupId, Long actorId, SessionInput input) {
        var group = access.lockGroup(groupId, actorId);
        return created(new StudySession(group, null, actorId, SessionDetails.validate(input, settings, groupId), clock.instant()), actorId);
    }
    public SessionDto createBuddy(Long connectionId, Long actorId, SessionInput input) {
        var connection = access.lockConnection(connectionId, actorId);
        return created(new StudySession(null, connection, actorId, SessionDetails.validate(input, settings, null), clock.instant()), actorId);
    }
    private SessionDto created(StudySession session, Long actorId) {
        sessions.saveAndFlush(session);
        events.publish(session, actorId, SessionEvents.Change.CREATED);
        return assembler.toDto(session, actorId, clock.instant());
    }
    @Transactional(readOnly = true)
    public SessionDto get(Long id, Long actorId) {
        authorize(id, actorId, false);
        return assembler.toDto(found(id), actorId, clock.instant());
    }
    public SessionDto edit(Long id, Long actorId, SessionInput input) {
        authorize(id, actorId, true);
        StudySession session = locked(id);
        expected(session, input.expectedVersion());
        session.edit(SessionDetails.validate(input, settings, session.getGroup() == null ? null : session.getGroup().getId()), clock.instant());
        sessions.saveAndFlush(session);
        events.publish(session, actorId, SessionEvents.Change.UPDATED);
        return assembler.toDto(session, actorId, clock.instant());
    }
    public SessionDto cancel(Long id, Long actorId, CancelSessionInput input) {
        authorize(id, actorId, true);
        StudySession session = locked(id);
        expected(session, input.expectedVersion());
        if (session.getStatus() == SessionStatus.CANCELLED) throw new ConflictException("This session is already cancelled");
        session.cancel(clock.instant());
        sessions.saveAndFlush(session);
        events.publish(session, actorId, SessionEvents.Change.CANCELLED);
        return assembler.toDto(session, actorId, clock.instant());
    }
    @Transactional(readOnly = true)
    public SessionAvailabilityDto groupAvailability(Long groupId, Long actorId) { return availability.forGroup(access.readGroup(groupId, actorId)); }
    @Transactional(readOnly = true)
    public SessionAvailabilityDto buddyAvailability(Long connectionId, Long actorId) { return availability.forConnection(access.readConnection(connectionId, actorId)); }
    @Transactional(readOnly = true)
    public CalendarExportDto calendar(Long id, Long actorId) {
        authorize(id, actorId, false);
        return calendar.export(found(id));
    }
    private List<SessionDto> dtos(List<StudySession> entities, Long actorId) {
        var now = clock.instant();
        return entities.stream().map(session -> assembler.toDto(session, actorId, now)).toList();
    }
    private void authorize(Long id, Long actorId, boolean write) {
        accounts.requireStudent(actorId);
        var parent = sessions.findParent(id).orElseThrow(() -> missing(id));
        if (!access.isRelated(parent.getGroupId(), parent.getConnectionId(), actorId)) throw missing(id);
        if (parent.getGroupId() != null) {
            if (write) {
                access.lockGroup(parent.getGroupId(), actorId);
            } else {
                access.readGroup(parent.getGroupId(), actorId);
            }
        } else {
            if (write) {
                access.lockConnection(parent.getConnectionId(), actorId);
            } else {
                access.readConnection(parent.getConnectionId(), actorId);
            }
        }
    }
    private StudySession found(Long id) { return sessions.findById(id).orElseThrow(() -> missing(id)); }
    private StudySession locked(Long id) { return sessions.findByIdForUpdate(id).orElseThrow(() -> missing(id)); }
    private NotFoundException missing(Long id) { return new NotFoundException("Study session " + id + " was not found"); }
    private void expected(StudySession session, Long version) {
        if (version == null || version < 0) throw new InvalidInputException("expectedVersion", "The saved session version is required");
        if (version != session.getVersion()) throw new ConflictException("This session changed; refresh before trying again");
    }
}
