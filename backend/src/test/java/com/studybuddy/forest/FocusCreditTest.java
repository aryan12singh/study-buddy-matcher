package com.studybuddy.forest;

import com.studybuddy.studyroom.PomodoroTimer;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FocusCreditTest {
    private static final Instant START = Instant.parse("2026-10-29T02:00:00Z");
    private static final PomodoroTimer IDLE = PomodoroTimer.idle(Duration.ofMinutes(25), Duration.ofMinutes(5));
    private static final PomodoroTimer RUNNING = IDLE.start(START);

    @Test
    void countsTheWholeStretchInsideAFocusPhase() {
        assertEquals(Duration.ofSeconds(2), FocusCredit.focusedBetween(RUNNING, START.plusSeconds(60), START.plusSeconds(62)));
    }

    @Test
    void idleAndPausedTimersEarnNothing() {
        assertEquals(Duration.ZERO, FocusCredit.focusedBetween(IDLE, START, START.plusSeconds(60)));
        var paused = RUNNING.pause(START.plusSeconds(30));
        assertEquals(Duration.ZERO, FocusCredit.focusedBetween(paused, START.plusSeconds(30), START.plusSeconds(90)));
    }

    @Test
    void timeBeforeTheTimerStartedIsNotCounted() {
        assertEquals(Duration.ofSeconds(10), FocusCredit.focusedBetween(RUNNING, START.minusSeconds(50), START.plusSeconds(10)));
        assertEquals(Duration.ZERO, FocusCredit.focusedBetween(RUNNING, START.minusSeconds(50), START));
    }

    @Test
    void breaksAreSkippedAcrossPhaseBoundaries() {
        Instant focusEnds = START.plus(Duration.ofMinutes(25));
        assertEquals(Duration.ofSeconds(30), FocusCredit.focusedBetween(RUNNING, focusEnds.minusSeconds(30), focusEnds.plusSeconds(60)));
        Instant breakEnds = focusEnds.plus(Duration.ofMinutes(5));
        assertEquals(Duration.ofSeconds(20), FocusCredit.focusedBetween(RUNNING, breakEnds.minusSeconds(40), breakEnds.plusSeconds(20)));
    }

    @Test
    void projectsLongRunsFromTheRightCycle() {
        Instant secondCycle = START.plus(Duration.ofMinutes(30 + 10));
        assertEquals(Duration.ofSeconds(90), FocusCredit.focusedBetween(RUNNING, secondCycle, secondCycle.plusSeconds(90)));
        assertEquals(Duration.ofMinutes(50), FocusCredit.focusedBetween(RUNNING, START, START.plus(Duration.ofMinutes(60))));
    }

    @Test
    void anEmptyOrBackwardStretchEarnsNothing() {
        assertEquals(Duration.ZERO, FocusCredit.focusedBetween(RUNNING, START.plusSeconds(60), START.plusSeconds(60)));
        assertEquals(Duration.ZERO, FocusCredit.focusedBetween(RUNNING, START.plusSeconds(60), START.plusSeconds(30)));
    }
}
