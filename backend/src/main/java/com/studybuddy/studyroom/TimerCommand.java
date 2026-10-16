package com.studybuddy.studyroom;

import java.time.Instant;

public enum TimerCommand {
    START { public PomodoroTimer apply(PomodoroTimer timer, Instant now) { return timer.start(now); } },
    PAUSE { public PomodoroTimer apply(PomodoroTimer timer, Instant now) { return timer.pause(now); } },
    RESUME { public PomodoroTimer apply(PomodoroTimer timer, Instant now) { return timer.resume(now); } },
    RESET { public PomodoroTimer apply(PomodoroTimer timer, Instant now) { return timer.reset(); } };

    public abstract PomodoroTimer apply(PomodoroTimer timer, Instant now);
}
