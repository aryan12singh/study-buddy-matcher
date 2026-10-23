package com.studybuddy.matchexplanation;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplanationQuotaTest {
    private static final Long STUDENT_ID = 7L;
    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");

    private static AiExplanationProperties limit(int calls) {
        return limits(calls, calls);
    }

    private static AiExplanationProperties limits(int calls, int callsPerStudent) {
        return new AiExplanationProperties(true, "http://localhost", "model", "key", Duration.ofSeconds(1),
                200, 600, calls, callsPerStudent, "Asia/Singapore", Duration.ofSeconds(60));
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

        assertTrue(quota.tryAcquire(STUDENT_ID));
        assertTrue(quota.tryAcquire(STUDENT_ID));
        assertFalse(quota.tryAcquire(STUDENT_ID));
    }

    @Test
    void resetsAtSingaporeMidnight() {
        MovableClock clock = new MovableClock(Instant.parse("2026-10-20T15:30:00Z")); // 23:30 in Singapore
        ExplanationQuota quota = new ExplanationQuota(limit(1), clock);

        assertTrue(quota.tryAcquire(STUDENT_ID));
        assertFalse(quota.tryAcquire(STUDENT_ID));
        clock.advance(Duration.ofHours(1)); // 00:30 the next day in Singapore
        assertTrue(quota.tryAcquire(STUDENT_ID));
    }

    @Test
    void oneStudentCannotUseUpTheWholeDailyAllowance() {
        ExplanationQuota quota = new ExplanationQuota(limits(5, 2), Clock.fixed(Instant.parse("2026-10-20T02:00:00Z"), SINGAPORE));

        assertTrue(quota.tryAcquire(STUDENT_ID));
        assertTrue(quota.tryAcquire(STUDENT_ID));
        assertFalse(quota.tryAcquire(STUDENT_ID));
        assertTrue(quota.tryAcquire(8L));
    }

    @Test
    void theSharedDailyLimitStillCapsEveryoneTogether() {
        ExplanationQuota quota = new ExplanationQuota(limits(2, 2), Clock.fixed(Instant.parse("2026-10-20T02:00:00Z"), SINGAPORE));

        assertTrue(quota.tryAcquire(1L));
        assertTrue(quota.tryAcquire(2L));
        assertFalse(quota.tryAcquire(3L));
    }

    @Test
    void eachStudentsAllowanceResetsAtSingaporeMidnight() {
        MovableClock clock = new MovableClock(Instant.parse("2026-10-20T15:30:00Z")); // 23:30 in Singapore
        ExplanationQuota quota = new ExplanationQuota(limits(10, 1), clock);

        assertTrue(quota.tryAcquire(STUDENT_ID));
        assertFalse(quota.tryAcquire(STUDENT_ID));
        clock.advance(Duration.ofHours(1));
        assertTrue(quota.tryAcquire(STUDENT_ID));
    }

    @Test
    void aZeroLimitNeverCalls() {
        assertFalse(new ExplanationQuota(limit(0), Clock.systemUTC()).tryAcquire(STUDENT_ID));
    }

    @Test
    void isReadyOnlyWhenEnabledWithAKey() {
        assertTrue(limit(1).ready());
        assertFalse(new AiExplanationProperties(false, "u", "m", "key", Duration.ofSeconds(1), 1, 1, 1, 1, "UTC", Duration.ofSeconds(60)).ready());
        assertFalse(new AiExplanationProperties(true, "u", "m", null, Duration.ofSeconds(1), 1, 1, 1, 1, "UTC", Duration.ofSeconds(60)).ready());
        assertFalse(new AiExplanationProperties(true, "u", "m", "  ", Duration.ofSeconds(1), 1, 1, 1, 1, "UTC", Duration.ofSeconds(60)).ready());
    }

    @Test
    void pausesAfterARateLimitWithoutSpendingTheDailyAllowance() {
        MovableClock clock = new MovableClock(Instant.parse("2026-10-20T02:00:00Z"));
        ExplanationQuota quota = new ExplanationQuota(limit(1), clock);

        quota.pauseAfterRateLimit();

        assertFalse(quota.tryAcquire(STUDENT_ID));
        clock.advance(Duration.ofSeconds(61));
        assertTrue(quota.tryAcquire(STUDENT_ID));
    }

    @Test
    void rejectsATimeoutThatIsNotPositive() {
        assertThrows(IllegalArgumentException.class, () -> new AiExplanationProperties(true, "u", "m", "key",
                Duration.ZERO, 1, 1, 1, 1, "UTC", Duration.ofSeconds(60)));
    }
}
