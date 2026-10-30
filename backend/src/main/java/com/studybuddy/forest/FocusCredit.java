package com.studybuddy.forest;

import com.studybuddy.studyroom.PomodoroTimer;
import com.studybuddy.studyroom.TimerPhase;
import com.studybuddy.studyroom.TimerStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Works out how much of a stretch of time a room's timer spent running a focus phase. */
public final class FocusCredit {
    private FocusCredit() {}

    /**
     * Focus time inside {@code [from, to)}, projected from the timer as it stands. A paused or idle
     * timer earns nothing, and time before a running timer's anchor is not counted.
     */
    public static Duration focusedBetween(PomodoroTimer timer, Instant from, Instant to) {
        Objects.requireNonNull(timer, "Timer is required");
        Objects.requireNonNull(from, "Start is required");
        Objects.requireNonNull(to, "End is required");
        if (timer.status() != TimerStatus.RUNNING || !to.isAfter(from) || !to.isAfter(timer.anchor())) {
            return Duration.ZERO;
        }
        Instant cursor = from.isBefore(timer.anchor()) ? timer.anchor() : from;
        PomodoroTimer state = timer.snapshot(cursor);
        TimerPhase phase = state.phase();
        Duration left = state.remainingAtAnchor();
        Duration focused = Duration.ZERO;
        while (cursor.isBefore(to)) {
            Instant phaseEnd = cursor.plus(left);
            Instant segmentEnd = phaseEnd.isBefore(to) ? phaseEnd : to;
            if (phase == TimerPhase.FOCUS) {
                focused = focused.plus(Duration.between(cursor, segmentEnd));
            }
            cursor = phaseEnd;
            phase = phase.next();
            left = phase == TimerPhase.FOCUS ? timer.focusDuration() : timer.breakDuration();
        }
        return focused;
    }
}
