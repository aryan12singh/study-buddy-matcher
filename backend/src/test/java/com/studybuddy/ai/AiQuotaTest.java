package com.studybuddy.ai;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The daily allowance, each student's share of it, the Singapore day boundary and the rate-limit pause. */
class AiQuotaTest {
    private static final Long STUDENT_ID = 7L;
    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");
    private static final Duration COOLDOWN = Duration.ofSeconds(60);
    private static final Clock MORNING = Clock.fixed(Instant.parse("2026-10-20T02:00:00Z"), SINGAPORE);

    private static AiQuota quota(int calls, Clock clock) {
        return quota(calls, calls, clock);
    }

    private static AiQuota quota(int calls, int callsPerStudent, Clock clock) {
        return new AiQuota(calls, callsPerStudent, COOLDOWN, clock);
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
        AiQuota quota = quota(2, MORNING);

        assertTrue(quota.tryAcquire(STUDENT_ID));
        assertTrue(quota.tryAcquire(STUDENT_ID));
        assertFalse(quota.tryAcquire(STUDENT_ID));
    }

    @Test
    void resetsAtSingaporeMidnight() {
        MovableClock clock = new MovableClock(Instant.parse("2026-10-20T15:30:00Z")); // 23:30 in Singapore
        AiQuota quota = quota(1, clock);

        assertTrue(quota.tryAcquire(STUDENT_ID));
        assertFalse(quota.tryAcquire(STUDENT_ID));
        clock.advance(Duration.ofHours(1)); // 00:30 the next day in Singapore
        assertTrue(quota.tryAcquire(STUDENT_ID));
    }

    @Test
    void oneStudentCannotUseUpTheWholeDailyAllowance() {
        AiQuota quota = quota(5, 2, MORNING);

        assertTrue(quota.tryAcquire(STUDENT_ID));
        assertTrue(quota.tryAcquire(STUDENT_ID));
        assertFalse(quota.tryAcquire(STUDENT_ID));
        assertTrue(quota.tryAcquire(8L));
    }

    @Test
    void theSharedDailyLimitStillCapsEveryoneTogether() {
        AiQuota quota = quota(2, 2, MORNING);

        assertTrue(quota.tryAcquire(1L));
        assertTrue(quota.tryAcquire(2L));
        assertFalse(quota.tryAcquire(3L));
    }

    @Test
    void eachStudentsAllowanceResetsAtSingaporeMidnight() {
        MovableClock clock = new MovableClock(Instant.parse("2026-10-20T15:30:00Z")); // 23:30 in Singapore
        AiQuota quota = quota(10, 1, clock);

        assertTrue(quota.tryAcquire(STUDENT_ID));
        assertFalse(quota.tryAcquire(STUDENT_ID));
        clock.advance(Duration.ofHours(1));
        assertTrue(quota.tryAcquire(STUDENT_ID));
    }

    @Test
    void aZeroLimitNeverCalls() {
        assertFalse(quota(0, Clock.systemUTC()).tryAcquire(STUDENT_ID));
    }

    @Test
    void pausesAfterARateLimitWithoutSpendingTheDailyAllowance() {
        MovableClock clock = new MovableClock(Instant.parse("2026-10-20T02:00:00Z"));
        AiQuota quota = quota(1, clock);

        quota.pauseAfterRateLimit();

        assertFalse(quota.tryAcquire(STUDENT_ID));
        clock.advance(Duration.ofSeconds(61));
        assertTrue(quota.tryAcquire(STUDENT_ID));
    }
}
