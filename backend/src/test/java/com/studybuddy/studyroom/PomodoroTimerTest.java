package com.studybuddy.studyroom;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PomodoroTimerTest {
    private static final Duration FOCUS = Duration.ofMinutes(25);
    private static final Duration BREAK = Duration.ofMinutes(5);
    private static final Instant START = Instant.parse("2026-10-09T09:00:00Z");

    @Test
    void idleTimerDoesNotAdvanceWithoutStarting() {
        PomodoroTimer timer = PomodoroTimer.idle(FOCUS, BREAK);

        assertEquals(TimerPhase.FOCUS, timer.phase());
        assertEquals(TimerStatus.IDLE, timer.status());
        assertEquals(FOCUS, timer.remainingAtAnchor());
        assertNull(timer.anchor());
        assertSame(timer, timer.snapshot(START.plus(Duration.ofDays(1))));
    }

    @Test
    void startDoesNotMutateTheIdleValue() {
        PomodoroTimer idle = PomodoroTimer.idle(FOCUS, BREAK);
        PomodoroTimer running = idle.start(START);

        assertEquals(TimerStatus.IDLE, idle.status());
        assertEquals(TimerStatus.RUNNING, running.status());
        assertEquals(START, running.anchor());
        assertEquals(FOCUS, running.remainingAtAnchor());
    }

    @Test
    void elapsedServerTimeReducesFocusWithoutTickCalls() {
        PomodoroTimer running = PomodoroTimer.idle(FOCUS, BREAK).start(START);
        PomodoroTimer current = running.snapshot(START.plus(Duration.ofMinutes(10)));

        assertEquals(TimerPhase.FOCUS, current.phase());
        assertEquals(Duration.ofMinutes(15), current.remainingAtAnchor());
        assertEquals(FOCUS, running.remainingAtAnchor());
    }

    @Test
    void focusBoundaryStartsACompleteBreak() {
        PomodoroTimer current = PomodoroTimer.idle(FOCUS, BREAK).start(START)
            .snapshot(START.plus(FOCUS));

        assertEquals(TimerPhase.BREAK, current.phase());
        assertEquals(BREAK, current.remainingAtAnchor());
        assertEquals(TimerStatus.RUNNING, current.status());
    }

    @Test
    void timeInsideTheBreakUsesTheBreakDuration() {
        PomodoroTimer current = PomodoroTimer.idle(FOCUS, BREAK).start(START)
            .snapshot(START.plus(FOCUS).plus(Duration.ofMinutes(2)));

        assertEquals(TimerPhase.BREAK, current.phase());
        assertEquals(Duration.ofMinutes(3), current.remainingAtAnchor());
    }

    @Test
    void completeCycleStartsTheNextFocus() {
        PomodoroTimer current = PomodoroTimer.idle(FOCUS, BREAK).start(START)
            .snapshot(START.plus(FOCUS).plus(BREAK));

        assertEquals(TimerPhase.FOCUS, current.phase());
        assertEquals(FOCUS, current.remainingAtAnchor());
    }

    @Test
    void reconnectCanSkipManyCompleteCycles() {
        PomodoroTimer current = PomodoroTimer.idle(FOCUS, BREAK).start(START)
            .snapshot(START.plus(Duration.ofDays(10000)).plus(Duration.ofMinutes(27)));

        assertEquals(TimerPhase.BREAK, current.phase());
        assertEquals(Duration.ofMinutes(3), current.remainingAtAnchor());
    }

    @Test
    void repeatedSnapshotsAgreeWithOneDirectSnapshot() {
        PomodoroTimer running = PomodoroTimer.idle(FOCUS, BREAK).start(START);
        Instant later = START.plus(Duration.ofMinutes(77)).plusMillis(325);
        PomodoroTimer repeated = running.snapshot(START.plus(Duration.ofMinutes(26)))
            .snapshot(START.plus(Duration.ofMinutes(32))).snapshot(later);

        assertEquals(running.snapshot(later), repeated);
    }

    @Test
    void snapshotsPreserveFractionalTime() {
        PomodoroTimer current = PomodoroTimer.idle(FOCUS, BREAK).start(START)
            .snapshot(START.plusMillis(125)).snapshot(START.plusMillis(375));

        assertEquals(FOCUS.minusMillis(375), current.remainingAtAnchor());
    }

    @Test
    void pauseFreezesTheCurrentPhaseAfterItsBoundary() {
        PomodoroTimer paused = PomodoroTimer.idle(FOCUS, BREAK).start(START)
            .pause(START.plus(Duration.ofMinutes(27)));

        assertEquals(TimerStatus.PAUSED, paused.status());
        assertEquals(TimerPhase.BREAK, paused.phase());
        assertEquals(Duration.ofMinutes(3), paused.remainingAtAnchor());
        assertNull(paused.anchor());
        assertSame(paused, paused.snapshot(START.plus(Duration.ofDays(1))));
    }

    @Test
    void resumeExcludesTimeSpentPaused() {
        PomodoroTimer paused = PomodoroTimer.idle(FOCUS, BREAK).start(START)
            .pause(START.plus(Duration.ofMinutes(27)));
        Instant resumedAt = START.plus(Duration.ofHours(3));
        PomodoroTimer current = paused.resume(resumedAt)
            .snapshot(resumedAt.plus(Duration.ofMinutes(1)));

        assertEquals(TimerPhase.BREAK, current.phase());
        assertEquals(Duration.ofMinutes(2), current.remainingAtAnchor());
    }

    @Test
    void resumedBreakTransitionsIntoTheNextFocus() {
        PomodoroTimer paused = PomodoroTimer.idle(FOCUS, BREAK).start(START)
            .pause(START.plus(Duration.ofMinutes(27)));
        Instant resumedAt = START.plus(Duration.ofHours(3));
        PomodoroTimer current = paused.resume(resumedAt)
            .snapshot(resumedAt.plus(Duration.ofMinutes(4)));

        assertEquals(TimerPhase.FOCUS, current.phase());
        assertEquals(Duration.ofMinutes(24), current.remainingAtAnchor());
    }

    @Test
    void resetRestoresTheConfiguredFocusInsteadOfASecondDefault() {
        Duration customFocus = Duration.ofMinutes(40);
        Duration customBreak = Duration.ofMinutes(8);
        PomodoroTimer reset = PomodoroTimer.idle(customFocus, customBreak)
            .start(START).pause(START.plus(Duration.ofMinutes(43))).reset();

        assertEquals(PomodoroTimer.idle(customFocus, customBreak), reset);
    }

    @Test
    void backwardClockDoesNotIncreaseRemainingTime() {
        PomodoroTimer running = PomodoroTimer.idle(FOCUS, BREAK).start(START);

        assertSame(running, running.snapshot(START.minusSeconds(1)));
    }

    @Test
    void idleRejectsPauseAndResume() {
        PomodoroTimer idle = PomodoroTimer.idle(FOCUS, BREAK);

        assertThrows(IllegalStateException.class, () -> idle.pause(START));
        assertThrows(IllegalStateException.class, () -> idle.resume(START));
    }

    @Test
    void runningRejectsAnotherStartAndResume() {
        PomodoroTimer running = PomodoroTimer.idle(FOCUS, BREAK).start(START);

        assertThrows(IllegalStateException.class, () -> running.start(START));
        assertThrows(IllegalStateException.class, () -> running.resume(START));
    }

    @Test
    void pausedRejectsAnotherPauseAndStart() {
        PomodoroTimer paused = PomodoroTimer.idle(FOCUS, BREAK).start(START).pause(START);

        assertThrows(IllegalStateException.class, () -> paused.pause(START));
        assertThrows(IllegalStateException.class, () -> paused.start(START));
    }

    @Test
    void nonPositivePhaseDurationsAreInvalid() {
        assertThrows(IllegalArgumentException.class, () -> PomodoroTimer.idle(Duration.ZERO, BREAK));
        assertThrows(IllegalArgumentException.class, () -> PomodoroTimer.idle(FOCUS, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> PomodoroTimer.idle(Duration.ofSeconds(-1), BREAK));
        assertThrows(IllegalArgumentException.class, () -> PomodoroTimer.idle(FOCUS, Duration.ofSeconds(-1)));
    }

    @Test
    void runningStateRequiresAnAnchor() {
        assertThrows(IllegalArgumentException.class, () -> new PomodoroTimer(FOCUS, BREAK,
            TimerPhase.FOCUS, TimerStatus.RUNNING, FOCUS, null));
    }

    @Test
    void stoppedStatesRejectAnAnchor() {
        assertThrows(IllegalArgumentException.class, () -> new PomodoroTimer(FOCUS, BREAK,
            TimerPhase.FOCUS, TimerStatus.IDLE, FOCUS, START));
        assertThrows(IllegalArgumentException.class, () -> new PomodoroTimer(FOCUS, BREAK,
            TimerPhase.FOCUS, TimerStatus.PAUSED, FOCUS, START));
    }

    @Test
    void remainingTimeMustBePositiveAndWithinThePhase() {
        assertThrows(IllegalArgumentException.class, () -> new PomodoroTimer(FOCUS, BREAK,
            TimerPhase.FOCUS, TimerStatus.PAUSED, Duration.ZERO, null));
        assertThrows(IllegalArgumentException.class, () -> new PomodoroTimer(FOCUS, BREAK,
            TimerPhase.BREAK, TimerStatus.PAUSED, BREAK.plusSeconds(1), null));
    }

    @Test
    void idleCannotContainAPartlyElapsedOrBreakPhase() {
        assertThrows(IllegalArgumentException.class, () -> new PomodoroTimer(FOCUS, BREAK,
            TimerPhase.FOCUS, TimerStatus.IDLE, FOCUS.minusSeconds(1), null));
        assertThrows(IllegalArgumentException.class, () -> new PomodoroTimer(FOCUS, BREAK,
            TimerPhase.BREAK, TimerStatus.IDLE, BREAK, null));
    }
}
