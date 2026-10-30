package com.studybuddy.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Limits password guessing. Each sign-in attempt for an email is reserved before the password is
 * checked, and only a successful sign-in gives it back, so {@code maxFailures} unsuccessful attempts
 * in a row lock that email for {@code lockout}, even for the right password. Reserving first means
 * parallel requests cannot all slip past the limit while their passwords are being checked.
 * Refused attempts are not counted, so the lock always ends one lockout after the last real attempt.
 * Unknown emails are counted the same way, so the response never reveals whether an account
 * exists. Kept in memory: a restart clears it, which suits a single coursework instance.
 */
@Component
public class LoginThrottle {
    private final int maxFailures;
    private final Duration lockout;
    private final Clock clock;
    private final Map<String, Attempts> attemptsByEmail = new HashMap<>();
    private Instant nextCleanup = Instant.MIN;

    private record Attempts(int count, Instant last) {
    }

    @Autowired
    public LoginThrottle(LoginThrottleProperties properties) {
        this(properties, Clock.systemUTC());
    }

    LoginThrottle(LoginThrottleProperties properties, Clock clock) {
        this.maxFailures = properties.maxFailures();
        this.lockout = properties.lockout();
        this.clock = clock;
    }

    /**
     * Counts one attempt for the normalized email, or throws when it is locked. Call before checking
     * the password; the attempt stays counted as a failure unless {@link #recordSuccess} follows.
     */
    public synchronized void beginAttempt(String email) {
        Instant now = clock.instant();
        forgetExpired(now);
        Attempts previous = attemptsByEmail.get(email);
        if (previous != null && isExpired(previous, now)) {
            previous = null;
        }
        if (previous != null && previous.count() >= maxFailures) {
            throw new TooManyLoginAttemptsException(Duration.between(now, previous.last().plus(lockout)));
        }
        attemptsByEmail.put(email, new Attempts(previous == null ? 1 : previous.count() + 1, now));
    }

    public synchronized void recordSuccess(String email) {
        attemptsByEmail.remove(email);
    }

    /** At most once per lockout, so guesses at many different emails neither grow the map nor rescan it every time. */
    private void forgetExpired(Instant now) {
        if (now.isBefore(nextCleanup)) {
            return;
        }
        attemptsByEmail.values().removeIf(attempts -> isExpired(attempts, now));
        nextCleanup = now.plus(lockout);
    }

    private boolean isExpired(Attempts attempts, Instant now) {
        return !now.isBefore(attempts.last().plus(lockout));
    }
}
