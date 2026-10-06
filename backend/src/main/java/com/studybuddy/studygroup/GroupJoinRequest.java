package com.studybuddy.studygroup;

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
 * A student's request to join a study group. Starts PENDING and moves once,
 * to ACCEPTED or REJECTED, through {@link #accept()} or {@link #reject()}; the
 * status has no public setter so no other transition is possible. A separate
 * state machine from {@code MatchRequest}, per the API contract.
 */
@Entity
@Table(name = "group_join_requests")
public class GroupJoinRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "study_group_id", nullable = false)
    private StudyGroup studyGroup;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupJoinRequestStatus status = GroupJoinRequestStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected GroupJoinRequest() {
    }

    public GroupJoinRequest(StudyGroup studyGroup, Student student, String message) {
        this.studyGroup = studyGroup;
        this.student = student;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public StudyGroup getStudyGroup() {
        return studyGroup;
    }

    public Student getStudent() {
        return student;
    }

    public String getMessage() {
        return message;
    }

    public GroupJoinRequestStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public boolean isPending() {
        return status == GroupJoinRequestStatus.PENDING;
    }

    public boolean belongsTo(Long studyGroupId) {
        return studyGroup.getId().equals(studyGroupId);
    }

    /**
     * @throws IllegalStateException if the request is no longer pending
     */
    public void accept() {
        respond(GroupJoinRequestStatus.ACCEPTED);
    }

    /**
     * @throws IllegalStateException if the request is no longer pending
     */
    public void reject() {
        respond(GroupJoinRequestStatus.REJECTED);
    }

    /**
     * Lets a caller check the state first, so a stale request is reported as
     * such rather than as whatever other rule happens to fail next.
     *
     * @throws IllegalStateException if the request is no longer pending
     */
    public void requirePending() {
        if (!isPending()) {
            throw new IllegalStateException(
                "Group join request " + id + " is already " + status + " and cannot be changed");
        }
    }

    private void respond(GroupJoinRequestStatus newStatus) {
        requirePending();
        this.status = newStatus;
        this.respondedAt = Instant.now();
    }
}
