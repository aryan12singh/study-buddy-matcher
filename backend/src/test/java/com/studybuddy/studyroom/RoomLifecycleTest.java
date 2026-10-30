package com.studybuddy.studyroom;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomLifecycleTest {
    private static final Instant NOW = Instant.parse("2026-10-24T09:00:00Z");
    private static final long GROUP_ID = 5L;
    private static final long OTHER_GROUP_ID = 6L;
    private static final long ALICE = 1L;
    private static final long BOB = 2L;

    private final StudyRoomRepository rooms = mock(StudyRoomRepository.class);
    private final RoomPresenceRepository presences = mock(RoomPresenceRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final RoomLifecycle lifecycle = new RoomLifecycle(rooms, presences, events, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void removingAMemberEndsTheirVisitToThatRoom() {
        StudyRoom room = mock(StudyRoom.class);
        when(rooms.findByStudyGroupId(GROUP_ID)).thenReturn(Optional.of(room));

        lifecycle.revokeMember(GROUP_ID, ALICE);

        verify(presences).deleteByStudyRoomStudyGroupIdAndStudentId(GROUP_ID, ALICE);
        verify(room).revokeRoles(ALICE);
        verify(events).publishEvent(new RoomVisitEnded(ALICE, GROUP_ID, NOW));
    }

    @Test
    void revokingAStudentEndsTheirVisitInEveryRoomTheyWereIn() {
        when(presences.findGroupIdsByStudentId(ALICE)).thenReturn(List.of(GROUP_ID, OTHER_GROUP_ID));
        when(rooms.findByHostIdOrCoHostId(ALICE, ALICE)).thenReturn(List.of());

        lifecycle.revokeStudent(ALICE);

        InOrder order = inOrder(presences, events);
        order.verify(presences).findGroupIdsByStudentId(ALICE);
        order.verify(events).publishEvent(new RoomVisitEnded(ALICE, GROUP_ID, NOW));
        order.verify(events).publishEvent(new RoomVisitEnded(ALICE, OTHER_GROUP_ID, NOW));
        order.verify(presences).deleteByStudentId(ALICE);
    }

    @Test
    void closingARoomEndsTheVisitOfEveryoneStillInIt() {
        StudyRoom room = mock(StudyRoom.class);
        when(rooms.findByStudyGroupId(GROUP_ID)).thenReturn(Optional.of(room));
        when(presences.findStudentIdsByGroupId(GROUP_ID)).thenReturn(List.of(ALICE, BOB));

        lifecycle.close(GROUP_ID);

        verify(events).publishEvent(new RoomVisitEnded(ALICE, GROUP_ID, NOW));
        verify(events).publishEvent(new RoomVisitEnded(BOB, GROUP_ID, NOW));
        verify(presences).deleteByStudyRoomStudyGroupId(GROUP_ID);
        verify(room).close();
    }

    @Test
    void closingAnEmptyRoomPublishesNothing() {
        when(rooms.findByStudyGroupId(GROUP_ID)).thenReturn(Optional.empty());
        when(presences.findStudentIdsByGroupId(GROUP_ID)).thenReturn(List.of());

        lifecycle.close(GROUP_ID);

        verify(events, never()).publishEvent(any(Object.class));
    }
}
