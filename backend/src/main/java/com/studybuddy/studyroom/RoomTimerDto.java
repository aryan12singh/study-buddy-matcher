package com.studybuddy.studyroom;

/**
 * The shared Pomodoro timer as of the response's server time: phase, status, time left and the
 * lengths of each phase, so every browser can count down in step.
 */
public record RoomTimerDto(TimerPhase phase, TimerStatus status, long remainingMillis,
    long focusMillis, long breakMillis) {}
