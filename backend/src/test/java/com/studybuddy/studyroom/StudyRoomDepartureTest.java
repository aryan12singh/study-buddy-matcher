package com.studybuddy.studyroom;

import com.studybuddy.student.Student;
import com.studybuddy.studygroup.StudyGroup;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudyRoomDepartureTest {
    private static final RoomSettings SETTINGS = new RoomSettings(25, 5, 120, 30, Duration.ofSeconds(30), Duration.ofSeconds(5));

    private StudyRoom room() {
        StudyGroup group = mock(StudyGroup.class);
        when(group.getMaxGroupSize()).thenReturn(4);
        return new StudyRoom(group, SETTINGS);
    }

    private Student student(long id) {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(id);
        return student;
    }

    @Test void handingOverToTheCoHostClearsTheCoHostRole() {
        StudyRoom room = room();
        Student host = student(1), coHost = student(2);
        room.configure(25, 5, 4, host, coHost);
        room.handOverHost(coHost);
        assertSame(coHost, room.getHost());
        assertNull(room.getCoHost());
    }

    @Test void handingOverToSomeoneElseKeepsTheCoHost() {
        StudyRoom room = room();
        Student host = student(1), coHost = student(2), other = student(3);
        room.configure(25, 5, 4, host, coHost);
        room.handOverHost(other);
        assertSame(other, room.getHost());
        assertSame(coHost, room.getCoHost());
    }

    @Test void endingTheSessionResetsTheTimer() {
        StudyRoom room = room();
        room.applyTimer(room.timer().start(Instant.parse("2026-10-24T09:00:00Z")));
        assertEquals(TimerStatus.RUNNING, room.timer().status());
        room.endSession();
        assertEquals(TimerStatus.IDLE, room.timer().status());
        assertEquals(TimerPhase.FOCUS, room.timer().phase());
    }
}
