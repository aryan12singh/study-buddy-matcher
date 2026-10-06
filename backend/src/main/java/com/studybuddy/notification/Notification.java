package com.studybuddy.notification;

import com.studybuddy.student.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Something that happened which a student should know about. Starts unread;
 * {@link #markRead()} is the only way to change that, and there is no way
 * back to unread.
 */
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    private Student recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", length = 30)
    private NotificationResourceType resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "event_key", length = 160)
    private String eventKey;

    public NotificationResourceType getResourceType() {
        return resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public String getEventKey() {
        return eventKey;
    }

    public Notification(Student recipient, NotificationType type, String message, NotificationResourceType resourceType,
        Long resourceId, String eventKey) {
        this(recipient, type, message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.eventKey = eventKey;
    }

    protected Notification() {
    }

    public Notification(Student recipient, NotificationType type, String message) {
        this.recipient = recipient;
        this.type = type;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public Student getRecipient() {
        return recipient;
    }

    public NotificationType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public boolean isFor(Long studentId) {
        return recipient.getId().equals(studentId);
    }

    /** Marking an already-read notification again changes nothing. */
    public void markRead() {
        this.read = true;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
