package com.studybuddy.notification;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByResourceTypeAndResourceId(NotificationResourceType type, Long resourceId);
    List<Notification> findByRecipientId(Long recipientId);
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    List<Notification> findByRecipientIdAndTypeInOrderByCreatedAtDesc(Long recipientId, Collection<NotificationType> types);
    List<Notification> findByRecipientIdAndReadFalse(Long recipientId);
    long countByRecipientIdAndReadFalse(Long recipientId);
}
