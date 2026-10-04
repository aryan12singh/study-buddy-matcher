package com.studybuddy.notification;

import com.studybuddy.student.Student;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The single entry point other services use to tell a student something
 * happened, and the student's own view of those notifications. Callers
 * decide the wording; this class only records it. The acting student's id is
 * passed in explicitly until authentication supplies it.
 */
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationAssembler notificationAssembler;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationAssembler notificationAssembler) {
        this.notificationRepository = notificationRepository;
        this.notificationAssembler = notificationAssembler;
    }

    public void notify(Student recipient, NotificationType type, String message) {
        notificationRepository.save(new Notification(recipient, type, message));
    }

    /** The student's notifications, newest first, read and unread. */
    @Transactional(readOnly = true)
    public List<NotificationDto> list(Long studentId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(studentId).stream()
                .map(notificationAssembler::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long studentId) {
        return notificationRepository.countByRecipientIdAndReadFalse(studentId);
    }

    /**
     * @throws NotificationNotFoundException if the notification does not exist
     * @throws NotificationNotAllowedException if it was sent to another student
     */
    public NotificationDto markRead(Long notificationId, Long studentId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if (!notification.isFor(studentId)) {
            throw new NotificationNotAllowedException(notificationId);
        }
        notification.markRead();
        return notificationAssembler.toDto(notification);
    }

    public void markAllRead(Long studentId) {
        notificationRepository.findByRecipientIdAndReadFalse(studentId).forEach(Notification::markRead);
    }
}
