package com.studybuddy.studyroom;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.ConflictException;
import com.studybuddy.student.Student;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomReactionServiceTest {
    private static final long GROUP_ID = 5L;
    private static final long ROOM_ID = 9L;
    private static final long STUDENT_ID = 1L;
    private static final Instant NOW = Instant.parse("2026-10-24T09:00:00Z");
    private static final RoomReactionSettings SETTINGS = new RoomReactionSettings(Duration.ofSeconds(8), 2, Duration.ofSeconds(10));

    private final StudyRoomRepository rooms = mock(StudyRoomRepository.class);
    private final RoomPresenceRepository presences = mock(RoomPresenceRepository.class);
    private final RoomReactionRepository reactions = mock(RoomReactionRepository.class);
    private final RoomAccess access = mock(RoomAccess.class);
    private final AccountAccess accounts = mock(AccountAccess.class);
    private final StudyRoom room = mock(StudyRoom.class);
    private final Student student = mock(Student.class);
    private RoomReactionService service;

    @BeforeEach
    void setUp() {
        service = new RoomReactionService(rooms, presences, reactions, access, accounts, SETTINGS, Clock.fixed(NOW, ZoneOffset.UTC));
        when(room.getId()).thenReturn(ROOM_ID);
        when(student.getId()).thenReturn(STUDENT_ID);
        when(student.getName()).thenReturn("Alice");
        when(rooms.findByStudyGroupId(GROUP_ID)).thenReturn(Optional.of(room));
        when(reactions.save(any(RoomReaction.class))).thenAnswer(call -> call.getArgument(0));
    }

    private void inRoom() {
        RoomPresence lease = mock(RoomPresence.class);
        when(lease.getStudent()).thenReturn(student);
        when(presences.findEligible(ROOM_ID, NOW)).thenReturn(List.of(lease));
    }

    @Test
    void sendsAReactionAfterLockingTheSenderBeforeCounting() {
        inRoom();
        when(reactions.countByStudyRoomIdAndStudentIdAndSentAtAfter(ROOM_ID, STUDENT_ID, NOW.minusSeconds(10))).thenReturn(1L);

        RoomReactionDto sent = service.send(GROUP_ID, STUDENT_ID, RoomReactionType.CLAP);

        assertEquals(RoomReactionType.CLAP, sent.type());
        assertEquals("Alice", sent.senderName());
        InOrder order = inOrder(accounts, reactions);
        order.verify(accounts).lockStudents(STUDENT_ID, STUDENT_ID);
        order.verify(reactions).countByStudyRoomIdAndStudentIdAndSentAtAfter(ROOM_ID, STUDENT_ID, NOW.minusSeconds(10));
        order.verify(reactions).save(any(RoomReaction.class));
    }

    @Test
    void refusesOnceTheBurstLimitIsReached() {
        inRoom();
        when(reactions.countByStudyRoomIdAndStudentIdAndSentAtAfter(ROOM_ID, STUDENT_ID, NOW.minusSeconds(10))).thenReturn(2L);

        assertThrows(ConflictException.class, () -> service.send(GROUP_ID, STUDENT_ID, RoomReactionType.FIRE));

        verify(reactions, never()).save(any());
    }

    @Test
    void refusesAMemberWhoIsNotInTheRoom() {
        when(presences.findEligible(ROOM_ID, NOW)).thenReturn(List.of());

        ConflictException error = assertThrows(ConflictException.class, () -> service.send(GROUP_ID, STUDENT_ID, RoomReactionType.HEART));

        assertEquals("Join the study room before sending a reaction", error.getMessage());
        verify(reactions, never()).countByStudyRoomIdAndStudentIdAndSentAtAfter(anyLong(), anyLong(), any());
        verify(reactions, never()).save(any());
    }

    @Test
    void refusesWhenTheGroupHasNoRoomYet() {
        when(rooms.findByStudyGroupId(GROUP_ID)).thenReturn(Optional.empty());

        assertThrows(ConflictException.class, () -> service.send(GROUP_ID, STUDENT_ID, RoomReactionType.HEART));

        verify(reactions, never()).save(any());
    }
}
