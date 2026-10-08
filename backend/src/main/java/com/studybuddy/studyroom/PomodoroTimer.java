package com.studybuddy.studyroom;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable focus/break state projected from server time rather than tick calls.
 * Services supply their clock and persist state after explicit controls; reading
 * a snapshot does not mutate the timer or require a background scheduler.
 */
public record PomodoroTimer(
    Duration focusDuration,
    Duration breakDuration,
    TimerPhase phase,
    TimerStatus status,
    Duration remainingAtAnchor,
    Instant anchor
) {
    public PomodoroTimer {
        requirePositive(focusDuration, "Focus duration");
        requirePositive(breakDuration, "Break duration");
        Objects.requireNonNull(phase, "Timer phase is required");
        Objects.requireNonNull(status, "Timer status is required");
        requirePositive(remainingAtAnchor, "Remaining duration");
        Duration phaseDuration = phase == TimerPhase.FOCUS ? focusDuration : breakDuration;
        if (remainingAtAnchor.compareTo(phaseDuration) > 0) {
            throw new IllegalArgumentException("Remaining duration exceeds the phase duration");
        }
        if ((status == TimerStatus.RUNNING) != (anchor != null)) {
            throw new IllegalArgumentException("Only a running timer has a time anchor");
        }
        if (status == TimerStatus.IDLE
            && (phase != TimerPhase.FOCUS || !remainingAtAnchor.equals(focusDuration))) {
            throw new IllegalArgumentException("An idle timer starts with a complete focus phase");
        }
    }

    public static PomodoroTimer idle(Duration focusDuration, Duration breakDuration) {
        return new PomodoroTimer(focusDuration, breakDuration, TimerPhase.FOCUS,
            TimerStatus.IDLE, focusDuration, null);
    }

    public PomodoroTimer start(Instant now) {
        requireStatus(TimerStatus.IDLE, "Only an idle timer can be started");
        return new PomodoroTimer(focusDuration, breakDuration, phase,
            TimerStatus.RUNNING, remainingAtAnchor, Objects.requireNonNull(now));
    }

    public PomodoroTimer pause(Instant now) {
        requireStatus(TimerStatus.RUNNING, "Only a running timer can be paused");
        PomodoroTimer current = snapshot(now);
        return new PomodoroTimer(focusDuration, breakDuration, current.phase,
            TimerStatus.PAUSED, current.remainingAtAnchor, null);
    }

    public PomodoroTimer resume(Instant now) {
        requireStatus(TimerStatus.PAUSED, "Only a paused timer can be resumed");
        return new PomodoroTimer(focusDuration, breakDuration, phase,
            TimerStatus.RUNNING, remainingAtAnchor, Objects.requireNonNull(now));
    }

    public PomodoroTimer reset() {
        return idle(focusDuration, breakDuration);
    }

    /** Skips complete cycles so reconnecting after a long absence has bounded work. */
    public PomodoroTimer snapshot(Instant now) {
        Objects.requireNonNull(now, "Server time is required");
        if (status != TimerStatus.RUNNING || now.isBefore(anchor)) {
            return this;
        }

        Duration elapsed = Duration.between(anchor, now);
        if (elapsed.compareTo(remainingAtAnchor) < 0) {
            return new PomodoroTimer(focusDuration, breakDuration, phase, status,
                remainingAtAnchor.minus(elapsed), now);
        }

        elapsed = elapsed.minus(remainingAtAnchor);
        Duration cycle = focusDuration.plus(breakDuration);
        long completeCycles = elapsed.dividedBy(cycle);
        elapsed = elapsed.minus(cycle.multipliedBy(completeCycles));
        TimerPhase currentPhase = phase.next();
        if (elapsed.compareTo(durationOf(currentPhase)) >= 0) {
            elapsed = elapsed.minus(durationOf(currentPhase));
            currentPhase = currentPhase.next();
        }
        return new PomodoroTimer(focusDuration, breakDuration, currentPhase, status,
            durationOf(currentPhase).minus(elapsed), now);
    }

    private Duration durationOf(TimerPhase timerPhase) {
        return timerPhase == TimerPhase.FOCUS ? focusDuration : breakDuration;
    }

    private void requireStatus(TimerStatus expected, String message) {
        if (status != expected) {
            throw new IllegalStateException(message);
        }
    }

    private static void requirePositive(Duration duration, String label) {
        Objects.requireNonNull(duration, label + " is required");
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException(label + " must be positive");
        }
    }
}
