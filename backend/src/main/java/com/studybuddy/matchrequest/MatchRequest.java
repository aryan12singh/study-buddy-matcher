package com.studybuddy.matchrequest;

import com.studybuddy.course.Course;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudyGoal;
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
 * A study-buddy request from one student to another. Starts PENDING and moves
 * once: to ACCEPTED or DECLINED when the receiver answers through {@link #accept()}
 * or {@link #decline()}, or to CANCELLED through {@link #cancel()} when either
 * account is deactivated first. The status has no public setter, so no other
 * transition is possible.
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
    private Instant createdAt = Instant.now();

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchRequestOrigin origin = MatchRequestOrigin.PROFILE;

    @ManyToOne
    @JoinColumn(name = "context_course_id")
    private Course contextCourse;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_study_goal", length = 30)
    private StudyGoal contextStudyGoal;

    public MatchRequestOrigin getOrigin() {
        return origin;
    }

    public Course getContextCourse() {
        return contextCourse;
    }

    public StudyGoal getContextStudyGoal() {
        return contextStudyGoal;
    }

    public MatchRequest(Student sender, Student receiver, String message, MatchRequestOrigin origin,
        Course course, StudyGoal goal) {
        this(sender, receiver, message);
        this.origin = origin;
        this.contextCourse = course;
        this.contextStudyGoal = goal;
    }

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRespondedAt() {
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

    /**
     * Closes a request nobody answered because one of the two accounts was deactivated.
     *
     * @throws IllegalStateException if the request is no longer pending
     */
    public void cancel() {
        respond(MatchRequestStatus.CANCELLED);
    }

    private void respond(MatchRequestStatus newStatus) {
        if (!isPending()) {
            throw new IllegalStateException(
                "Match request " + id + " is already " + status + " and cannot be changed");
        }
        this.status = newStatus;
        this.respondedAt = Instant.now();
    }
}
