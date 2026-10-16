package com.studybuddy.studysession;

import com.studybuddy.notification.NotificationRepository;
import com.studybuddy.notification.NotificationResourceType;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Called after existing parent/account locks, before memberships disappear or connections end. */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class StudySessionLifecycle {
    private final StudySessionRepository sessions;
    private final SessionEvents events;
    private final Clock clock;
    private final NotificationRepository notifications;
    public StudySessionLifecycle(StudySessionRepository sessions, SessionEvents events, Clock clock, NotificationRepository notifications) {
        this.sessions = sessions; this.events = events; this.clock = clock; this.notifications = notifications;
    }
    public void cancelForGroup(Long groupId) {
        cancel(sessions.findByGroupIdAndStatus(groupId, SessionStatus.SCHEDULED));
    }
    public void cancelForConnection(Long connectionId) {
        cancel(sessions.findByConnectionIdAndStatus(connectionId, SessionStatus.SCHEDULED));
    }
    public void cancelForStudent(Long studentId) {
        cancel(sessions.findScheduledBuddyForStudent(studentId));
    }
    public void deleteForGroup(Long groupId) {
        delete(sessions.findByGroupIdOrderByStartAtAsc(groupId));
    }
    public void deleteForConnection(Long connectionId) {
        delete(sessions.findByConnectionIdOrderByStartAtAsc(connectionId));
    }
    private void delete(List<StudySession> affected) {
        for (StudySession session : affected) {
            notifications.deleteAll(notifications.findByResourceTypeAndResourceId(NotificationResourceType.SESSION, session.getId()));
        }
        sessions.deleteAll(affected);
        sessions.flush();
    }
    private void cancel(List<StudySession> affected) {
        var now = clock.instant();
        for (StudySession session : affected) {
            if (session.schedule().occurrences().stream().noneMatch(slot -> slot.endAt().isAfter(now))) continue;
            session.cancel(now);
            sessions.saveAndFlush(session);
            events.publish(session, null, SessionEvents.Change.CANCELLED);
        }
    }
}
