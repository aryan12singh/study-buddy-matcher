package com.studybuddy.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginThrottleTest {
    private static final String EMAIL = "student@example.test";

    private final MovableClock clock = new MovableClock(Instant.parse("2026-10-21T02:00:00Z"));
    private final LoginThrottle throttle = new LoginThrottle(new LoginThrottleProperties(3, Duration.ofMinutes(15)), clock);

    private static final class MovableClock extends Clock {
        private Instant now;

        MovableClock(Instant start) {
            now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    /** Unsuccessful attempts: each one is begun and never followed by a success. */
    private void fail(int times) {
        for (int attempt = 0; attempt < times; attempt++) {
            throttle.beginAttempt(EMAIL);
        }
    }

    @Test
    void allowsSignInBelowTheFailureLimit() {
        fail(2);

        assertDoesNotThrow(() -> throttle.beginAttempt(EMAIL));
    }

    @Test
    void refusesSignInOnceTheLimitIsReached() {
        fail(3);

        var refused = assertThrows(TooManyLoginAttemptsException.class, () -> throttle.beginAttempt(EMAIL));
        assertEquals("Too many incorrect sign-in attempts. Try again in 15 minutes.", refused.getMessage());
    }

    @Test
    void reportsTheRemainingWaitRoundedUpToAMinute() {
        fail(3);
        clock.advance(Duration.ofMinutes(14).plusSeconds(30));

        var refused = assertThrows(TooManyLoginAttemptsException.class, () -> throttle.beginAttempt(EMAIL));
        assertEquals("Too many incorrect sign-in attempts. Try again in 1 minute.", refused.getMessage());
    }

    @Test
    void allowsSignInAgainOnceTheLockoutHasPassed() {
        fail(3);
        clock.advance(Duration.ofMinutes(15));

        assertDoesNotThrow(() -> throttle.beginAttempt(EMAIL));
    }

    @Test
    void startsCountingAgainAfterAQuietLockoutPeriod() {
        fail(2);
        clock.advance(Duration.ofMinutes(15));
        fail(2);

        assertDoesNotThrow(() -> throttle.beginAttempt(EMAIL));
    }

    @Test
    void aSuccessfulSignInClearsEarlierFailures() {
        fail(2);
        throttle.recordSuccess(EMAIL);
        fail(2);

        assertDoesNotThrow(() -> throttle.beginAttempt(EMAIL));
    }

    @Test
    void refusedAttemptsDoNotExtendTheLockout() {
        fail(3);
        clock.advance(Duration.ofMinutes(10));
        assertThrows(TooManyLoginAttemptsException.class, () -> throttle.beginAttempt(EMAIL));
        clock.advance(Duration.ofMinutes(5));

        assertDoesNotThrow(() -> throttle.beginAttempt(EMAIL));
    }

    @Test
    void attemptsStillBeingCheckedCountTowardsTheLimit() {
        // Three requests arrive together; none has finished checking its password yet.
        throttle.beginAttempt(EMAIL);
        throttle.beginAttempt(EMAIL);
        throttle.beginAttempt(EMAIL);

        assertThrows(TooManyLoginAttemptsException.class, () -> throttle.beginAttempt(EMAIL));
    }

    @Test
    void neverReportsAZeroMinuteWait() {
        fail(3);
        clock.advance(Duration.ofMinutes(15).minusNanos(1));

        var refused = assertThrows(TooManyLoginAttemptsException.class, () -> throttle.beginAttempt(EMAIL));
        assertEquals("Too many incorrect sign-in attempts. Try again in 1 minute.", refused.getMessage());
    }

    @Test
    void countsEachEmailSeparately() {
        fail(3);

        assertDoesNotThrow(() -> throttle.beginAttempt("someone-else@example.test"));
    }

    @Test
    void rejectsSettingsThatWouldNeverOrAlwaysLock() {
        assertThrows(IllegalArgumentException.class, () -> new LoginThrottleProperties(3, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new LoginThrottleProperties(3, Duration.ofMinutes(-1)));
    }
}
