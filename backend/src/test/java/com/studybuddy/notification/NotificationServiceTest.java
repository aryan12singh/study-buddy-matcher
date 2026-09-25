package com.studybuddy.notification;

import com.studybuddy.student.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void notifySavesAnUnreadNotificationForTheRecipient() {
        Student bob = new Student(null, "Bob", "SCIS", "Information Systems", 2, "+65 9000 0002");

        notificationService.notify(bob, NotificationType.MATCH_REQUEST_RECEIVED, "Alice sent you a study-buddy request");

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(saved.capture());
        assertSame(bob, saved.getValue().getRecipient());
        assertEquals(NotificationType.MATCH_REQUEST_RECEIVED, saved.getValue().getType());
        assertEquals("Alice sent you a study-buddy request", saved.getValue().getMessage());
        assertFalse(saved.getValue().isRead());
    }
}
