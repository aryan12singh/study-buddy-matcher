package com.studybuddy.notification;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.student.Student;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository notifications;
    private final NotificationAssembler assembler;
    private final AccountAccess access;

    public NotificationService(NotificationRepository notifications, NotificationAssembler assembler, AccountAccess access) {
        this.notifications = notifications;
        this.assembler = assembler;
        this.access = access;
    }

    /** Internal event write: called within the successful domain/account transaction. */
    public void notify(Student recipient, NotificationType type, String message, NotificationResourceType resourceType, Long resourceId, String eventKey) {
        notifications.save(new Notification(recipient, type, message, resourceType, resourceId, eventKey));
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> list(Long actorId) {
        return list(actorId, NotificationFilter.ALL);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> list(Long actorId, NotificationFilter filter) {
        access.requireStudent(actorId);
        return assembler.toDtos(filter == NotificationFilter.ALL
            ? notifications.findByRecipientIdOrderByCreatedAtDesc(actorId)
            : notifications.findByRecipientIdAndTypeInOrderByCreatedAtDesc(actorId, filter.types()));
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long actorId) {
        access.requireStudent(actorId);
        return notifications.countByRecipientIdAndReadFalse(actorId);
    }

    public NotificationDto markRead(Long id, Long actorId) {
        access.lockStudents(actorId, actorId);
        Notification notification = notifications.findById(id).orElseThrow(() -> new NotificationNotFoundException(id));
        if (!notification.isFor(actorId)) {
            throw new NotificationNotAllowedException(id);
        }
        notification.markRead();
        return assembler.toDto(notification);
    }

    public void markAllRead(Long actorId) {
        access.lockStudents(actorId, actorId);
        notifications.findByRecipientIdAndReadFalse(actorId).forEach(Notification::markRead);
    }
}
