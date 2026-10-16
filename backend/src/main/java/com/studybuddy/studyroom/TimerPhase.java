package com.studybuddy.studyroom;

public enum TimerPhase {
    FOCUS,
    BREAK;

    public TimerPhase next() {
        return this == FOCUS ? BREAK : FOCUS;
    }
}
