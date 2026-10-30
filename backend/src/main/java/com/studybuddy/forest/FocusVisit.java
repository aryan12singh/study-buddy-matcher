package com.studybuddy.forest;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;

/**
 * One continuous stay in a study room. Focus time is added only through
 * {@link FocusVisitRepository#credit}, which guards against two tabs crediting the same stretch,
 * so this entity has no setters.
 */
@Entity
@Table(name = "focus_visits")
public class FocusVisit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    @Column(name = "study_group_id")
    private Long studyGroupId;
    @Column(name = "group_name", nullable = false)
    private String groupName;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;
    @Column(name = "focused_millis", nullable = false)
    private long focusedMillis;
    @Column(name = "ended_at")
    private Instant endedAt;
    @Column(nullable = false)
    private int variant;
    @Column(nullable = false)
    private int plot;

    protected FocusVisit() {}

    /** A visit is over once it was ended, or once its last tick is older than a room lease. */
    public boolean isOver(Instant now, Duration leaseLifetime) {
        return endedAt != null || lastSeenAt.plus(leaseLifetime).isBefore(now);
    }

    /** When the tree went in: the explicit end, or else the last moment the student was seen. */
    public Instant plantedAt() {
        return endedAt != null ? endedAt : lastSeenAt;
    }

    public Duration focused() { return Duration.ofMillis(focusedMillis); }
    public Long getId() { return id; }
    public Long getStudentId() { return studentId; }
    public Long getStudyGroupId() { return studyGroupId; }
    public String getGroupName() { return groupName; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public Instant getEndedAt() { return endedAt; }
    public int getVariant() { return variant; }
    public int getPlot() { return plot; }
}
