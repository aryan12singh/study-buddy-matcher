package com.studybuddy.studyroom;

import com.studybuddy.student.Student;
import jakarta.persistence.*;
import java.time.Instant;

/** One reaction sent in a room. Rows are short-lived and never edited, so there are no setters. */
@Entity
@Table(name = "room_reactions")
public class RoomReaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "study_room_id", nullable = false)
    private StudyRoom studyRoom;
    @ManyToOne @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private RoomReactionType type;
    @Column(nullable = false)
    private Instant sentAt;

    protected RoomReaction() {}

    public RoomReaction(StudyRoom room, Student student, RoomReactionType type, Instant sentAt) {
        this.studyRoom = room;
        this.student = student;
        this.type = type;
        this.sentAt = sentAt;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public RoomReactionType getType() { return type; }
    public Instant getSentAt() { return sentAt; }
}
