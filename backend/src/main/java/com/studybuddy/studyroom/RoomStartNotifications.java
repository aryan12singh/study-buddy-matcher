package com.studybuddy.studyroom;

import com.studybuddy.notification.NotificationResourceType;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.notification.NotificationType;
import com.studybuddy.student.Student;
import com.studybuddy.studygroup.GroupMembership;
import com.studybuddy.studygroup.GroupMembershipRepository;
import com.studybuddy.studygroup.StudyGroup;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Invites group members who are not in the room when a shared session starts. A member who has
 * not yet read the previous invitation for this group is not sent another, so restarting the
 * timer cannot pile notices up. Invitations are a courtesy: they run in their own transaction
 * after the start commits, and a failure is logged rather than undoing the start.
 */
@Component
public class RoomStartNotifications {
    private static final Logger LOG = LoggerFactory.getLogger(RoomStartNotifications.class);
    private final StudyRoomRepository rooms;
    private final GroupMembershipRepository memberships;
    private final RoomPresenceRepository presences;
    private final NotificationService notifications;
    private final TransactionTemplate separateTransaction;

    public RoomStartNotifications(StudyRoomRepository rooms, GroupMembershipRepository memberships,
        RoomPresenceRepository presences, NotificationService notifications, PlatformTransactionManager transactions) {
        this.rooms = rooms;
        this.memberships = memberships;
        this.presences = presences;
        this.notifications = notifications;
        this.separateTransaction = new TransactionTemplate(transactions);
        this.separateTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** The try covers the whole separate transaction, so a failure reported at its commit is caught too. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoomStarted(RoomStarted event) {
        try {
            separateTransaction.executeWithoutResult(status ->
                rooms.findById(event.roomId()).ifPresent(room -> publish(room, event.actorId(), event.startedAt())));
        } catch (RuntimeException failure) {
            LOG.warn("Study room {} started, but its invitations could not be sent", event.roomId(), failure);
        }
    }

    void publish(StudyRoom room, Long actorId, Instant startedAt) {
        StudyGroup group = room.getStudyGroup();
        Set<Long> present = presences.findEligible(room.getId(), startedAt).stream()
            .map(presence -> presence.getStudent().getId())
            .collect(Collectors.toSet());
        for (GroupMembership membership : memberships.findByStudyGroupId(group.getId())) {
            Student member = membership.getStudent();
            if (member.getId().equals(actorId) || present.contains(member.getId()) || !member.getUser().isActive()
                || notifications.hasUnread(member.getId(), NotificationType.GROUP_STUDY_ROOM_STARTED, group.getId())) {
                continue;
            }
            notifications.notify(member, NotificationType.GROUP_STUDY_ROOM_STARTED,
                group.getName() + ": the study room has started. Join in when you are ready.",
                NotificationResourceType.GROUP, group.getId(),
                "study-room:" + room.getId() + ":started:" + startedAt.toEpochMilli() + ":" + member.getId());
        }
    }
}
