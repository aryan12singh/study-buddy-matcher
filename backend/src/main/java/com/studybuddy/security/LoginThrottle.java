package com.studybuddy.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Limits password guessing. After {@code maxFailures} wrong passwords in a row for one email,
 * sign-in for that email is refused for {@code lockout}, even with the right password. Refused
 * attempts are not counted, so the lock always ends one lockout after the last real failure.
 * Unknown emails are counted the same way, so the response never reveals whether an account
 * exists. Kept in memory: a restart clears it, which suits a single coursework instance.
 */
@Component
public class LoginThrottle {
    private final int maxFailures;
    private final Duration lockout;
    private final Clock clock;
    private final Map<String, Failures> failuresByEmail = new HashMap<>();

    private record Failures(int count, Instant lastFailure) {
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

    /** Throws when the email is locked; call with the normalized email before checking the password. */
    public synchronized void requireAllowed(String email) {
        Failures failures = current(email, clock.instant());
        if (failures != null && failures.count() >= maxFailures) {
            Duration remaining = Duration.between(clock.instant(), failures.lastFailure().plus(lockout));
            throw new TooManyLoginAttemptsException(minutesRoundedUp(remaining));
        }
    }

    public synchronized void recordFailure(String email) {
        Instant now = clock.instant();
        // Forget expired entries so guesses at many different emails cannot grow the map forever.
        failuresByEmail.values().removeIf(failures -> isExpired(failures, now));
        Failures previous = failuresByEmail.get(email);
        int count = previous == null ? 1 : previous.count() + 1;
        failuresByEmail.put(email, new Failures(count, now));
    }

    public synchronized void recordSuccess(String email) {
        failuresByEmail.remove(email);
    }

    private Failures current(String email, Instant now) {
        Failures failures = failuresByEmail.get(email);
        return failures == null || isExpired(failures, now) ? null : failures;
    }

    private boolean isExpired(Failures failures, Instant now) {
        return !now.isBefore(failures.lastFailure().plus(lockout));
    }

    private static long minutesRoundedUp(Duration remaining) {
        long minutes = remaining.toMinutes();
        return remaining.equals(Duration.ofMinutes(minutes)) ? minutes : minutes + 1;
    }
}
