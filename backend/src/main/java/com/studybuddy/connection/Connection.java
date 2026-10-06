package com.studybuddy.connection;

import com.studybuddy.student.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * An accepted study-buddy connection between two students. {@code endedAt}
 * being null is what "active" means; no separate status flag is kept in sync.
 * Ending is one-way, through {@link #end()}; there is no setter that reopens
 * a connection.
 */
@Entity
@Table(name = "connections")
public class Connection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_a_id", nullable = false)
    private Student studentA;

    @ManyToOne
    @JoinColumn(name = "student_b_id", nullable = false)
    private Student studentB;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;

    protected Connection() {
    }

    public Connection(Student studentA, Student studentB) {
        this.studentA = studentA;
        this.studentB = studentB;
    }

    public Long getId() {
        return id;
    }

    public Student getStudentA() {
        return studentA;
    }

    public Student getStudentB() {
        return studentB;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public boolean isActive() {
        return endedAt == null;
    }

    public boolean involves(Long studentId) {
        return studentA.getId().equals(studentId) || studentB.getId().equals(studentId);
    }

    /**
     * The participant on the other side from {@code studentId}.
     *
     * @throws IllegalArgumentException if the student is not in this connection
     */
    public Student otherStudent(Long studentId) {
        if (studentA.getId().equals(studentId)) {
            return studentB;
        }
        if (studentB.getId().equals(studentId)) {
            return studentA;
        }
        throw new IllegalArgumentException("Student " + studentId + " is not part of connection " + id);
    }

    /**
     * @throws IllegalStateException if the connection has already ended
     */
    public void end() {
        if (!isActive()) {
            throw new IllegalStateException("Connection " + id + " has already ended");
        }
        this.endedAt = Instant.now();
    }
}
