package com.studybuddy.studygroup;

import com.studybuddy.student.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * A student's confirmed membership in a study group. Created once a
 * {@link GroupJoinRequest} is accepted; a separate table from that request,
 * per the API contract note that join requests are their own state machine.
 */
@Entity
@Table(name = "group_memberships", uniqueConstraints = @UniqueConstraint(columnNames = {"study_group_id", "student_id"}))
public class GroupMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "study_group_id", nullable = false)
    private StudyGroup studyGroup;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    protected GroupMembership() {
    }

    public GroupMembership(StudyGroup studyGroup, Student student) {
        this.studyGroup = studyGroup;
        this.student = student;
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

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}
