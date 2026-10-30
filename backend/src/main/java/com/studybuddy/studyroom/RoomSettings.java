package com.studybuddy.studyroom;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Group room settings bound from {@code app.study-room}: default and maximum focus and break
 * lengths, how long a presence lease lasts and how often tabs poll. A lease must outlive the poll
 * interval, or present students would keep dropping out; startup fails otherwise.
 */
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
