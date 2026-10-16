package com.studybuddy.studyroom;

import com.studybuddy.student.Student;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** One lease per browser tab; several leases count as one student toward capacity. */
@Entity
@Table(name = "room_presences", uniqueConstraints = @UniqueConstraint(columnNames = {"study_room_id", "student_id", "client_id"}))
public class RoomPresence {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "study_room_id", nullable = false)
    private StudyRoom studyRoom;
    @ManyToOne @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @Column(nullable = false)
    private UUID clientId;
    @Column(nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private Instant observedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PresenceState presence;

    protected RoomPresence() {}

    public RoomPresence(StudyRoom room, Student student, UUID clientId, Instant now, Instant expiresAt) {
        this.studyRoom = room;
        this.student = student;
        this.clientId = clientId;
        renew(PresenceState.PRESENT, now, expiresAt);
    }

    public void renew(PresenceState presence, Instant now, Instant expiresAt) {
        this.presence = presence;
        observedAt = now;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public UUID getClientId() { return clientId; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getObservedAt() { return observedAt; }
    public PresenceState getPresence() { return presence; }
}
