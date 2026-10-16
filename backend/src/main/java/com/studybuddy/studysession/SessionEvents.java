package com.studybuddy.studysession;

import com.studybuddy.notification.NotificationResourceType;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.notification.NotificationType;
import com.studybuddy.student.Student;
import com.studybuddy.studygroup.GroupMembership;
import com.studybuddy.studygroup.GroupMembershipRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/** Recipient scope is evaluated in the same successful session/lifecycle transaction. */
@Component
public class SessionEvents {
    public enum Change { CREATED, UPDATED, CANCELLED }
    private final GroupMembershipRepository memberships;
    private final NotificationService notifications;
    public SessionEvents(GroupMembershipRepository memberships, NotificationService notifications) {
        this.memberships = memberships; this.notifications = notifications;
    }
    public void publish(StudySession session, Long actorId, Change change) {
        boolean group = session.getGroup() != null;
        List<Student> recipients = group ? memberships.findByStudyGroupId(session.getGroup().getId()).stream()
            .map(GroupMembership::getStudent).toList()
            : List.of(session.getConnection().getStudentA(), session.getConnection().getStudentB());
        NotificationType type = switch (change) {
            case CREATED -> group ? NotificationType.GROUP_SESSION_CREATED : NotificationType.BUDDY_SESSION_CREATED;
            case UPDATED -> group ? NotificationType.GROUP_SESSION_UPDATED : NotificationType.BUDDY_SESSION_UPDATED;
            case CANCELLED -> group ? NotificationType.GROUP_SESSION_CANCELLED : NotificationType.BUDDY_SESSION_CANCELLED;
        };
        String verb = switch (change) { case CREATED -> "scheduled"; case UPDATED -> "updated"; case CANCELLED -> "cancelled"; };
        for (Student recipient : recipients) {
            if (!recipient.getUser().isActive() || recipient.getId().equals(actorId)) continue;
            notifications.notify(recipient, type, "Study session " + verb + ": " + session.getTitle(),
                NotificationResourceType.SESSION, session.getId(),
                "session:" + session.getId() + ":" + session.getVersion() + ":" + change + ":" + recipient.getId());
        }
    }
}
