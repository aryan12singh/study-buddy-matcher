package com.studybuddy.studysession;

import com.studybuddy.connection.Connection;
import com.studybuddy.studygroup.StudyGroup;
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
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "study_sessions")
public class StudySession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private long version;
    @Column(name = "calendar_uid", nullable = false, updatable = false) private UUID calendarUid = UUID.randomUUID();
    @ManyToOne @JoinColumn(name = "study_group_id") private StudyGroup group;
    @ManyToOne @JoinColumn(name = "connection_id") private Connection connection;
    @Column(name = "created_by") private Long createdBy;
    @Column(nullable = false) private String title;
    @JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME) @Column(name = "start_local", nullable = false) private LocalDateTime startLocal;
    @JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME) @Column(name = "end_local", nullable = false) private LocalDateTime endLocal;
    @Column(name = "start_at", nullable = false) private Instant startAt;
    @Column(name = "end_at", nullable = false) private Instant endAt;
    @Column(name = "time_zone", nullable = false, length = 100) private String timeZone;
    @Column(name = "occurrence_count", nullable = false) private int occurrenceCount;
    @Column(length = 4000) private String agenda;
    private String location;
    @Column(name = "room_link", length = 2048) private String roomLink;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SessionStatus status = SessionStatus.SCHEDULED;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "cancelled_at") private Instant cancelledAt;

    protected StudySession() { }
    public StudySession(StudyGroup group, Connection connection, Long actorId, SessionDetails details, Instant now) {
        if ((group == null) == (connection == null)) throw new IllegalArgumentException("A session belongs to exactly one group or connection");
        this.group = group;
        this.connection = connection;
        this.createdBy = actorId;
        this.createdAt = now;
        apply(details, now);
    }
    public void edit(SessionDetails details, Instant now) {
        if (status == SessionStatus.CANCELLED) throw new IllegalStateException("A cancelled session cannot be edited");
        apply(details, now);
    }
    private void apply(SessionDetails details, Instant now) {
        title = details.title();
        startLocal = details.schedule().startLocal();
        endLocal = details.schedule().endLocal();
        timeZone = details.schedule().timeZone();
        occurrenceCount = details.schedule().occurrenceCount();
        var first = details.schedule().occurrences().getFirst();
        startAt = first.startAt();
        endAt = first.endAt();
        agenda = details.agenda();
        location = details.location();
        roomLink = details.roomLink();
        updatedAt = now;
    }
    public void cancel(Instant now) {
        if (status == SessionStatus.CANCELLED) return;
        status = SessionStatus.CANCELLED;
        cancelledAt = now;
        updatedAt = now;
    }
    public SessionSchedule schedule() { return new SessionSchedule(startLocal, endLocal, timeZone, occurrenceCount); }
    public Long getId() { return id; }
    public long getVersion() { return version; }
    public UUID getCalendarUid() { return calendarUid; }
    public StudyGroup getGroup() { return group; }
    public Connection getConnection() { return connection; }
    public String getTitle() { return title; }
    public LocalDateTime getStartLocal() { return startLocal; }
    public LocalDateTime getEndLocal() { return endLocal; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public String getTimeZone() { return timeZone; }
    public int getOccurrenceCount() { return occurrenceCount; }
    public String getAgenda() { return agenda; }
    public String getLocation() { return location; }
    public String getRoomLink() { return roomLink; }
    public SessionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
}
