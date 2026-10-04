package com.studybuddy.notification;

import com.studybuddy.student.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Long ALICE_ID = 1L;
    private static final Long BOB_ID = 2L;
    private static final Long NOTIFICATION_ID = 30L;

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;
    private Student bob;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, new NotificationAssembler());
        bob = new Student(null, "Bob", "SCIS", "Information Systems", 2, "+65 9000 0002");
        ReflectionTestUtils.setField(bob, "id", BOB_ID);
    }

    @Test
    void notifySavesAnUnreadNotificationForTheRecipient() {
        notificationService.notify(bob, NotificationType.MATCH_REQUEST_RECEIVED, "Alice sent you a study-buddy request");

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(saved.capture());
        assertSame(bob, saved.getValue().getRecipient());
        assertEquals(NotificationType.MATCH_REQUEST_RECEIVED, saved.getValue().getType());
        assertEquals("Alice sent you a study-buddy request", saved.getValue().getMessage());
        assertFalse(saved.getValue().isRead());
    }

    // --- list ---

    @Test
    void listReturnsTheRepositorysNewestFirstOrder() {
        Notification newer = notification(31L, NotificationType.MATCH_REQUEST_ACCEPTED);
        Notification older = notification(NOTIFICATION_ID, NotificationType.MATCH_REQUEST_RECEIVED);
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(BOB_ID)).thenReturn(List.of(newer, older));

        List<NotificationDto> notifications = notificationService.list(BOB_ID);

        assertEquals(List.of(31L, NOTIFICATION_ID), notifications.stream().map(NotificationDto::id).toList());
        assertEquals(NotificationType.MATCH_REQUEST_ACCEPTED, notifications.get(0).type());
        assertFalse(notifications.get(0).read());
    }

    @Test
    void listIsEmptyForStudentWithNoNotifications() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(BOB_ID)).thenReturn(List.of());

        assertTrue(notificationService.list(BOB_ID).isEmpty());
    }

    // --- unreadCount ---

    @Test
    void unreadCountComesFromTheRecipientsUnreadNotifications() {
        when(notificationRepository.countByRecipientIdAndReadFalse(BOB_ID)).thenReturn(3L);

        assertEquals(3L, notificationService.unreadCount(BOB_ID));
    }

    // --- markRead ---

    @Test
    void recipientCanMarkTheirNotificationRead() {
        Notification notification = givenNotificationForBob();

        NotificationDto dto = notificationService.markRead(NOTIFICATION_ID, BOB_ID);

        assertTrue(dto.read());
        assertTrue(notification.isRead());
    }

    @Test
    void markingAnAlreadyReadNotificationLeavesItRead() {
        Notification notification = givenNotificationForBob();
        notification.markRead();

        assertTrue(notificationService.markRead(NOTIFICATION_ID, BOB_ID).read());
    }

    @Test
    void anotherStudentCannotMarkYourNotificationRead() {
        Notification notification = givenNotificationForBob();

        assertThrows(NotificationNotAllowedException.class,
                () -> notificationService.markRead(NOTIFICATION_ID, ALICE_ID));

        assertFalse(notification.isRead());
    }

    @Test
    void markingUnknownNotificationThrowsNotFound() {
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.markRead(NOTIFICATION_ID, BOB_ID));
    }

    // --- markAllRead ---

    @Test
    void markAllReadMarksEveryUnreadNotificationOfTheStudent() {
        Notification first = notification(NOTIFICATION_ID, NotificationType.MATCH_REQUEST_RECEIVED);
        Notification second = notification(31L, NotificationType.GROUP_MEMBER_REMOVED);
        when(notificationRepository.findByRecipientIdAndReadFalse(BOB_ID)).thenReturn(List.of(first, second));

        notificationService.markAllRead(BOB_ID);

        assertTrue(first.isRead());
        assertTrue(second.isRead());
    }

    private Notification givenNotificationForBob() {
        Notification notification = notification(NOTIFICATION_ID, NotificationType.MATCH_REQUEST_RECEIVED);
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
        return notification;
    }

    private Notification notification(Long id, NotificationType type) {
        Notification notification = new Notification(bob, type, "Something happened");
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }
}
