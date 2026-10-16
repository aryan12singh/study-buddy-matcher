package com.studybuddy.studyroom;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.study-room")
public record RoomSettings(int defaultFocusMinutes, int defaultBreakMinutes,
    int maxFocusMinutes, int maxBreakMinutes, Duration leaseLifetime, Duration pollInterval) {
    public RoomSettings {
        if (defaultFocusMinutes < 1 || defaultBreakMinutes < 1 || maxFocusMinutes < defaultFocusMinutes
            || maxBreakMinutes < defaultBreakMinutes || pollInterval == null || pollInterval.isNegative()
            || pollInterval.isZero() || leaseLifetime == null || leaseLifetime.compareTo(pollInterval) <= 0) {
            throw new IllegalArgumentException("Invalid study room configuration");
        }
    }
}
