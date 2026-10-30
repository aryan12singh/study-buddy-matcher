package com.studybuddy.studyroom;

import java.time.Instant;

/**
 * The controls of the shared room timer. Each constant applies its change to an immutable
 * {@link PomodoroTimer}, so the service needs no switch on the command.
 */
public enum TimerCommand {
    START { public PomodoroTimer apply(PomodoroTimer timer, Instant now) { return timer.start(now); } },
    PAUSE { public PomodoroTimer apply(PomodoroTimer timer, Instant now) { return timer.pause(now); } },
    RESUME { public PomodoroTimer apply(PomodoroTimer timer, Instant now) { return timer.resume(now); } },
    RESET { public PomodoroTimer apply(PomodoroTimer timer, Instant now) { return timer.reset(); } };

    public abstract PomodoroTimer apply(PomodoroTimer timer, Instant now);
}
