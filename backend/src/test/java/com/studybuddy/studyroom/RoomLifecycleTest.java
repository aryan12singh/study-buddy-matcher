package com.studybuddy.studyroom;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomLifecycleTest {
    private static final Instant NOW = Instant.parse("2026-10-24T09:00:00Z");
    private static final long GROUP_ID = 5L;
    private static final long ALICE = 1L;

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
    void revokingAStudentEndsTheirVisitsEvenWithoutACurrentTab() {
        when(rooms.findByHostIdOrCoHostId(ALICE, ALICE)).thenReturn(List.of());

        lifecycle.revokeStudent(ALICE);

        verify(presences).deleteByStudentId(ALICE);
        verify(events).publishEvent(new StudentRoomAccessRevoked(ALICE, NOW));
    }

    @Test
    void closingARoomEndsEveryVisitToIt() {
        StudyRoom room = mock(StudyRoom.class);
        when(rooms.findByStudyGroupId(GROUP_ID)).thenReturn(Optional.of(room));

        lifecycle.close(GROUP_ID);

        verify(presences).deleteByStudyRoomStudyGroupId(GROUP_ID);
        verify(room).close();
        verify(events).publishEvent(new StudyRoomClosed(GROUP_ID, NOW));
    }
}
