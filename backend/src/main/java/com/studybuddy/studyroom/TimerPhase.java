package com.studybuddy.studyroom;

/** The two alternating phases of a Pomodoro cycle. */
public enum TimerPhase {
    FOCUS,
    BREAK;

    public TimerPhase next() {
        return this == FOCUS ? BREAK : FOCUS;
    }
}
