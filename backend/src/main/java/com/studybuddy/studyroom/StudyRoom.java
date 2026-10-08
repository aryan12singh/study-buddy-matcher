package com.studybuddy.studyroom;

import com.studybuddy.student.Student;
import com.studybuddy.studygroup.StudyGroup;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;

/** Durable shared room state; presence leases are separate from timer controls. */
@Entity
@Table(name = "study_rooms", uniqueConstraints = @UniqueConstraint(columnNames = "study_group_id"))
public class StudyRoom {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne @JoinColumn(name = "study_group_id", nullable = false)
    private StudyGroup studyGroup;
    @ManyToOne @JoinColumn(name = "host_id")
    private Student host;
    @ManyToOne @JoinColumn(name = "co_host_id")
    private Student coHost;
    @Version
    private long version;
    @Column(nullable = false)
    private int focusMinutes;
    @Column(nullable = false)
    private int breakMinutes;
    @Column(nullable = false)
    private int participantLimit;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TimerPhase phase;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TimerStatus timerStatus;
    @Column(nullable = false)
    private long remainingMillis;
    private Instant timerAnchor;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private AudioPreset audioPreset = AudioPreset.CALM_MUSIC;
    @Column(nullable = false)
    private boolean audioPlaying;

    protected StudyRoom() {}

    public StudyRoom(StudyGroup group, RoomSettings settings) {
        studyGroup = group;
        focusMinutes = settings.defaultFocusMinutes();
        breakMinutes = settings.defaultBreakMinutes();
        participantLimit = group.getMaxGroupSize();
        applyTimer(PomodoroTimer.idle(Duration.ofMinutes(focusMinutes), Duration.ofMinutes(breakMinutes)));
    }

    public PomodoroTimer timer() {
        return new PomodoroTimer(Duration.ofMinutes(focusMinutes), Duration.ofMinutes(breakMinutes),
            phase, timerStatus, Duration.ofMillis(remainingMillis), timerAnchor);
    }

    public void applyTimer(PomodoroTimer timer) {
        phase = timer.phase();
        timerStatus = timer.status();
        remainingMillis = timer.remainingAtAnchor().toMillis();
        timerAnchor = timer.anchor();
    }

    public void configure(int focus, int breakLength, int limit, Student newHost, Student newCoHost) {
        boolean changedDurations = focus != focusMinutes || breakLength != breakMinutes;
        if (changedDurations && timerStatus != TimerStatus.IDLE) {
            throw new IllegalStateException("Reset the timer before changing its durations");
        }
        focusMinutes = focus;
        breakMinutes = breakLength;
        participantLimit = limit;
        host = newHost;
        coHost = newCoHost;
        if (changedDurations) applyTimer(PomodoroTimer.idle(Duration.ofMinutes(focus), Duration.ofMinutes(breakLength)));
    }

    public void selectAudio(AudioPreset preset, boolean playing) { audioPreset = preset; audioPlaying = playing; }
    public Long getId() { return id; }
    public StudyGroup getStudyGroup() { return studyGroup; }
    public Student getHost() { return host; }
    public Student getCoHost() { return coHost; }
    public long getVersion() { return version; }
    public int getFocusMinutes() { return focusMinutes; }
    public int getBreakMinutes() { return breakMinutes; }
    public int getParticipantLimit() { return participantLimit; }
    public AudioPreset getAudioPreset() { return audioPreset; }
    public boolean isAudioPlaying() { return audioPlaying; }
}
