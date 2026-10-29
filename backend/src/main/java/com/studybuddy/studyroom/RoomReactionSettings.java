package com.studybuddy.studyroom;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * How long a reaction stays in the room snapshot, and how many one student may send in a
 * stretch of time. The window must outlast the poll interval so every polling tab sees it.
 */
@ConfigurationProperties("app.study-room.reactions")
public record RoomReactionSettings(Duration window, int burstLimit, Duration burstWindow) {
    public RoomReactionSettings {
        if (window == null || window.isNegative() || window.isZero() || burstLimit < 1
            || burstWindow == null || burstWindow.isNegative() || burstWindow.isZero()) {
            throw new IllegalArgumentException("Invalid study room reaction configuration");
        }
    }

    /** Rows older than this are no longer shown or counted, so they can be deleted. */
    public Duration retention() {
        return window.compareTo(burstWindow) > 0 ? window : burstWindow;
    }
}
