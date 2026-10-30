package com.studybuddy.forest;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.studyroom.PomodoroTimer;
import com.studybuddy.studyroom.RoomSettings;
import com.studybuddy.studyroom.RoomVisitEnded;
import com.studybuddy.studyroom.RoomVisitTicked;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForestServiceTest {
    private static final long STUDENT = 1L;
    private static final long GROUP = 5L;
    private static final long VISIT = 40L;
    private static final Instant NOW = Instant.parse("2026-10-24T09:00:00Z");
    private static final Duration LEASE = Duration.ofSeconds(90);
    private static final RoomSettings ROOMS = new RoomSettings(25, 5, 180, 60, LEASE, Duration.ofSeconds(2));
    private static final PomodoroTimer FOCUSING = PomodoroTimer.idle(Duration.ofMinutes(25), Duration.ofMinutes(5))
        .start(NOW.minus(Duration.ofMinutes(10)));

    private final FocusVisitRepository visits = mock(FocusVisitRepository.class);
    private final TreeLottery lottery = mock(TreeLottery.class);
    private ForestService service;

    @BeforeEach
    void setUp() {
        service = new ForestService(visits, mock(ForestAssembler.class), mock(TreeGrowth.class), ROOMS,
            mock(AccountAccess.class), lottery, Clock.fixed(NOW, ZoneOffset.UTC));
        when(lottery.draw()).thenReturn(new TreeDraw(1, 2));
    }

    private static RoomVisitTicked tickAt(Instant at) {
        return new RoomVisitTicked(STUDENT, GROUP, "Revision group", FOCUSING, at);
    }

    private static FocusVisit openVisit(Instant lastSeenAt) {
        FocusVisit visit = new FocusVisit();
        ReflectionTestUtils.setField(visit, "id", VISIT);
        ReflectionTestUtils.setField(visit, "studentId", STUDENT);
        ReflectionTestUtils.setField(visit, "studyGroupId", GROUP);
        ReflectionTestUtils.setField(visit, "startedAt", lastSeenAt.minus(Duration.ofMinutes(5)));
        ReflectionTestUtils.setField(visit, "lastSeenAt", lastSeenAt);
        return visit;
    }

    @Test
    void aTickFromBeforeTheStudentLeftDoesNotReopenTheEndedVisit() {
        when(visits.existsByStudentIdAndStudyGroupIdAndEndedAtGreaterThan(STUDENT, GROUP, NOW)).thenReturn(true);

        service.recordTick(tickAt(NOW));

        verify(visits, never()).open(anyLong(), anyLong(), any(), any(), anyInt(), anyInt());
        verify(visits, never()).credit(anyLong(), any(), any(), anyLong());
    }

    @Test
    void aVisitWhoseLeaseLapsedEndsAtItsLastTickAndANewOneStarts() {
        Instant lastSeen = NOW.minus(LEASE).minusSeconds(1);
        when(visits.findByStudentIdAndStudyGroupIdAndEndedAtIsNull(STUDENT, GROUP)).thenReturn(Optional.of(openVisit(lastSeen)));

        service.recordTick(tickAt(NOW));

        verify(visits).end(VISIT, lastSeen);
        verify(visits).open(STUDENT, GROUP, "Revision group", NOW, 1, 2);
        verify(visits, never()).credit(anyLong(), any(), any(), anyLong());
    }

    @Test
    void aTickNoLaterThanTheLastOneCreditsNothing() {
        when(visits.findByStudentIdAndStudyGroupIdAndEndedAtIsNull(STUDENT, GROUP)).thenReturn(Optional.of(openVisit(NOW)));

        service.recordTick(tickAt(NOW));
        service.recordTick(tickAt(NOW.minusSeconds(5)));

        verify(visits, never()).credit(anyLong(), any(), any(), anyLong());
        verify(visits, never()).end(anyLong(), any());
        verify(visits, never()).open(anyLong(), anyLong(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void aLaterTickCreditsTheFocusTimeSinceTheLastOne() {
        Instant lastSeen = NOW.minusSeconds(30);
        when(visits.findByStudentIdAndStudyGroupIdAndEndedAtIsNull(STUDENT, GROUP)).thenReturn(Optional.of(openVisit(lastSeen)));

        service.recordTick(tickAt(NOW));

        verify(visits).credit(VISIT, lastSeen, NOW, 30_000L);
    }

    @Test
    void theFirstTickOpensAVisit() {
        when(visits.findByStudentIdAndStudyGroupIdAndEndedAtIsNull(STUDENT, GROUP)).thenReturn(Optional.empty());

        service.recordTick(tickAt(NOW));

        verify(visits).open(STUDENT, GROUP, "Revision group", NOW, 1, 2);
    }

    @Test
    void endingAVisitClosesTheOpenOne() {
        when(visits.findByStudentIdAndStudyGroupIdAndEndedAtIsNull(STUDENT, GROUP)).thenReturn(Optional.of(openVisit(NOW)));

        service.endVisit(new RoomVisitEnded(STUDENT, GROUP, NOW));

        verify(visits).end(VISIT, NOW);
    }
}
