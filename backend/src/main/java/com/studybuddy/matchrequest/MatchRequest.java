package com.studybuddy.matchrequest;

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

import java.time.LocalDateTime;

/**
 * A study-buddy request from one student to another. Starts PENDING and moves
 * once, to ACCEPTED or DECLINED, through {@link #accept()} or {@link #decline()};
 * the status has no public setter so no other transition is possible.
 */
@Entity
@Table(name = "match_requests")
public class MatchRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private Student sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private Student receiver;

    @Column
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchRequestStatus status = MatchRequestStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    protected MatchRequest() {
    }

    public MatchRequest(Student sender, Student receiver, String message) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public Student getSender() {
        return sender;
    }

    public Student getReceiver() {
        return receiver;
    }

    public String getMessage() {
        return message;
    }

    public MatchRequestStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public boolean isPending() {
        return status == MatchRequestStatus.PENDING;
    }

    public boolean involves(Long studentId) {
        return sender.getId().equals(studentId) || receiver.getId().equals(studentId);
    }

    public boolean isReceiver(Long studentId) {
        return receiver.getId().equals(studentId);
    }

    /**
     * @throws IllegalStateException if the request is no longer pending
     */
    public void accept() {
        respond(MatchRequestStatus.ACCEPTED);
    }

    /**
     * @throws IllegalStateException if the request is no longer pending
     */
    public void decline() {
        respond(MatchRequestStatus.DECLINED);
    }

    private void respond(MatchRequestStatus newStatus) {
        if (!isPending()) {
            throw new IllegalStateException(
                    "Match request " + id + " is already " + status + " and cannot be changed");
        }
        this.status = newStatus;
        this.respondedAt = LocalDateTime.now();
    }
}
