package com.studybuddy.ai;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiQuotaTest {
    private static final Long STUDENT_ID = 7L;
    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");

    private static AiAssistantProperties limit(int calls) {
        return limits(calls, calls);
    }

    private static AiAssistantProperties limits(int calls, int callsPerStudent) {
        return new AiAssistantProperties(true, "http://localhost", "model", "key", Duration.ofSeconds(1),
                4000, calls, callsPerStudent, "Asia/Singapore", Duration.ofSeconds(60));
    }

    private static AiAssistantProperties withKey(boolean enabled, String apiKey) {
        return new AiAssistantProperties(enabled, "u", "m", apiKey, Duration.ofSeconds(1), 1, 1, 1, "UTC",
                Duration.ofSeconds(60));
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
        AiQuota quota = new AiQuota(limit(2), Clock.fixed(Instant.parse("2026-10-20T02:00:00Z"), SINGAPORE));

        assertTrue(quota.tryAcquire(STUDENT_ID));
        assertTrue(quota.tryAcquire(STUDENT_ID));
        assertFalse(quota.tryAcquire(STUDENT_ID));
    }

    @Test
    void resetsAtSingaporeMidnight() {
        MovableClock clock = new MovableClock(Instant.parse("2026-10-20T15:30:00Z")); // 23:30 in Singapore
        AiQuota quota = new AiQuota(limit(1), clock);

        assertTrue(quota.tryAcquire(STUDENT_ID));
        assertFalse(quota.tryAcquire(STUDENT_ID));
        clock.advance(Duration.ofHours(1)); // 00:30 the next day in Singapore
        assertTrue(quota.tryAcquire(STUDENT_ID));
    }

    @Test
    void oneStudentCannotUseUpTheWholeDailyAllowance() {
        AiQuota quota = new AiQuota(limits(5, 2), Clock.fixed(Instant.parse("2026-10-20T02:00:00Z"), SINGAPORE));

        assertTrue(quota.tryAcquire(STUDENT_ID));
        assertTrue(quota.tryAcquire(STUDENT_ID));
        assertFalse(quota.tryAcquire(STUDENT_ID));
        assertTrue(quota.tryAcquire(8L));
    }

    @Test
    void theSharedDailyLimitStillCapsEveryoneTogether() {
        AiQuota quota = new AiQuota(limits(2, 2), Clock.fixed(Instant.parse("2026-10-20T02:00:00Z"), SINGAPORE));

        assertTrue(quota.tryAcquire(1L));
        assertTrue(quota.tryAcquire(2L));
        assertFalse(quota.tryAcquire(3L));
    }

    @Test
    void aZeroLimitNeverCalls() {
        assertFalse(new AiQuota(limit(0), Clock.systemUTC()).tryAcquire(STUDENT_ID));
    }

    @Test
    void pausesAfterARateLimitWithoutSpendingTheDailyAllowance() {
        MovableClock clock = new MovableClock(Instant.parse("2026-10-20T02:00:00Z"));
        AiQuota quota = new AiQuota(limit(1), clock);

        quota.pauseAfterRateLimit();

        assertFalse(quota.tryAcquire(STUDENT_ID));
        clock.advance(Duration.ofSeconds(61));
        assertTrue(quota.tryAcquire(STUDENT_ID));
    }

    @Test
    void isReadyOnlyWhenEnabledWithAKey() {
        assertTrue(withKey(true, "key").ready());
        assertFalse(withKey(false, "key").ready());
        assertFalse(withKey(true, null).ready());
        assertFalse(withKey(true, "  ").ready());
    }

    @Test
    void neverPrintsTheKey() {
        assertFalse(withKey(true, "secret-key").toString().contains("secret-key"));
    }

    @Test
    void rejectsATimeoutThatIsNotPositive() {
        assertThrows(IllegalArgumentException.class, () -> new AiAssistantProperties(true, "u", "m", "key",
                Duration.ZERO, 1, 1, 1, "UTC", Duration.ofSeconds(60)));
    }
}
