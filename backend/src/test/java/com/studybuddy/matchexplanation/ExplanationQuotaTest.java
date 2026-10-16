package com.studybuddy.matchexplanation;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplanationQuotaTest {
    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");

    private static AiExplanationProperties limit(int calls) {
        return new AiExplanationProperties(true, "http://localhost", "model", "key", Duration.ofSeconds(1),
                200, 600, calls, "Asia/Singapore");
    }

    private static final class MovableClock extends Clock {
        private Instant now;

        MovableClock(Instant start) {
            now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override public ZoneId getZone() { return SINGAPORE; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void allowsCallsUpToTheDailyLimit() {
        ExplanationQuota quota = new ExplanationQuota(limit(2), Clock.fixed(Instant.parse("2026-10-20T02:00:00Z"), SINGAPORE));

        assertTrue(quota.tryAcquire());
        assertTrue(quota.tryAcquire());
        assertFalse(quota.tryAcquire());
    }

    @Test
    void resetsAtSingaporeMidnight() {
        MovableClock clock = new MovableClock(Instant.parse("2026-10-20T15:30:00Z")); // 23:30 in Singapore
        ExplanationQuota quota = new ExplanationQuota(limit(1), clock);

        assertTrue(quota.tryAcquire());
        assertFalse(quota.tryAcquire());
        clock.advance(Duration.ofHours(1)); // 00:30 the next day in Singapore
        assertTrue(quota.tryAcquire());
    }

    @Test
    void aZeroLimitNeverCalls() {
        assertFalse(new ExplanationQuota(limit(0), Clock.systemUTC()).tryAcquire());
    }

    @Test
    void isReadyOnlyWhenEnabledWithAKey() {
        assertTrue(limit(1).ready());
        assertFalse(new AiExplanationProperties(false, "u", "m", "key", Duration.ofSeconds(1), 1, 1, 1, "UTC").ready());
        assertFalse(new AiExplanationProperties(true, "u", "m", null, Duration.ofSeconds(1), 1, 1, 1, "UTC").ready());
        assertFalse(new AiExplanationProperties(true, "u", "m", "  ", Duration.ofSeconds(1), 1, 1, 1, "UTC").ready());
    }
}
