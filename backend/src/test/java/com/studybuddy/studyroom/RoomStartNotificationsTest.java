package com.studybuddy.studyroom;

import com.studybuddy.notification.NotificationResourceType;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.notification.NotificationType;
import com.studybuddy.student.Student;
import com.studybuddy.studygroup.GroupMembership;
import com.studybuddy.studygroup.GroupMembershipRepository;
import com.studybuddy.studygroup.StudyGroup;
import com.studybuddy.user.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoomStartNotificationsTest {
    private static final Long ROOM_ID = 9L;
    private static final Long GROUP_ID = 5L;
    private static final Instant STARTED_AT = Instant.parse("2026-10-24T02:00:00Z");

    @Mock private GroupMembershipRepository memberships;
    @Mock private RoomPresenceRepository presences;
    @Mock private NotificationService notifications;
    @Mock private StudyRoomRepository rooms;
    @Mock private PlatformTransactionManager transactions;

    @Test
    void invitesOnlyActiveMembersWhoAreAwayAndHaveNoUnreadInvitation() {
        Student host = student(1L, true);
        Student inRoom = student(2L, true);
        Student away = student(3L, true);
        Student deactivated = student(4L, true);
        when(deactivated.getUser().isActive()).thenReturn(false);
        Student alreadyInvited = student(5L, true);
        StudyRoom room = room(host, inRoom, away, deactivated, alreadyInvited);
        RoomPresence presence = mock(RoomPresence.class);
        when(presence.getStudent()).thenReturn(inRoom);
        when(presences.findEligible(ROOM_ID, STARTED_AT)).thenReturn(List.of(presence));
        when(notifications.hasUnread(5L, NotificationType.GROUP_STUDY_ROOM_STARTED, GROUP_ID)).thenReturn(true);

        notifier().publish(room, host.getId(), STARTED_AT);

        verify(notifications).notify(away, NotificationType.GROUP_STUDY_ROOM_STARTED,
            "Revision crew: the study room has started. Join in when you are ready.",
            NotificationResourceType.GROUP, GROUP_ID, "study-room:9:started:" + STARTED_AT.toEpochMilli() + ":3");
        for (Student skipped : List.of(host, inRoom, deactivated, alreadyInvited)) {
            verify(notifications, never()).notify(eq(skipped), any(), anyString(), any(), anyLong(), anyString());
        }
    }

    @Test
    void sendsNothingWhenEveryoneIsAlreadyInTheRoom() {
        Student host = student(1L, true);
        Student member = student(2L, true);
        StudyRoom room = room(host, member);
        RoomPresence presence = mock(RoomPresence.class);
        when(presence.getStudent()).thenReturn(member);
        when(presences.findEligible(ROOM_ID, STARTED_AT)).thenReturn(List.of(presence));

        notifier().publish(room, host.getId(), STARTED_AT);

        verify(notifications, never()).notify(any(), any(), anyString(), any(), anyLong(), anyString());
    }

    @Test
    void aFailedInvitationIsLoggedAndNeverReachesTheStart() {
        StudyRoom room = room(student(1L, true), student(2L, true));
        when(rooms.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(presences.findEligible(ROOM_ID, STARTED_AT)).thenReturn(List.of());
        doThrow(new DataIntegrityViolationException("notifications_type_check"))
            .when(notifications).notify(any(), any(), anyString(), any(), anyLong(), anyString());

        assertDoesNotThrow(() -> notifier().onRoomStarted(new RoomStarted(ROOM_ID, 1L, STARTED_AT)));
        verify(transactions).rollback(any());
    }

    private RoomStartNotifications notifier() {
        return new RoomStartNotifications(rooms, memberships, presences, notifications, transactions);
    }

    private StudyRoom room(Student... members) {
        StudyGroup group = mock(StudyGroup.class);
        when(group.getId()).thenReturn(GROUP_ID);
        when(group.getName()).thenReturn("Revision crew");
        StudyRoom room = mock(StudyRoom.class);
        when(room.getId()).thenReturn(ROOM_ID);
        when(room.getStudyGroup()).thenReturn(group);
        List<GroupMembership> rows = java.util.Arrays.stream(members).map(member -> {
            GroupMembership membership = mock(GroupMembership.class);
            when(membership.getStudent()).thenReturn(member);
            return membership;
        }).toList();
        when(memberships.findByStudyGroupId(GROUP_ID)).thenReturn(rows);
        return room;
    }

    private Student student(Long id, boolean active) {
        User user = mock(User.class);
        when(user.isActive()).thenReturn(active);
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(id);
        when(student.getUser()).thenReturn(user);
        return student;
    }
}
