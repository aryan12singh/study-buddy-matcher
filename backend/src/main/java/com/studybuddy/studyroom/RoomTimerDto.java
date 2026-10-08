package com.studybuddy.studyroom;

public record RoomTimerDto(TimerPhase phase, TimerStatus status, long remainingMillis,
    long focusMillis, long breakMillis) {}
