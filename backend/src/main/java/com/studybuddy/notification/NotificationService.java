package com.studybuddy.notification;

import com.studybuddy.student.Student;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single entry point other services use to tell a student something
 * happened. Callers decide the wording; this class only records it.
 */
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void notify(Student recipient, NotificationType type, String message) {
        notificationRepository.save(new Notification(recipient, type, message));
    }
}
