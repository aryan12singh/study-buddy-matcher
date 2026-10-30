package com.studybuddy.security;

import com.studybuddy.common.error.ApiException;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/** A locked email; the response says when to try again in words and in a {@code Retry-After} header. */
public class TooManyLoginAttemptsException extends ApiException {
    private final long retryAfterSeconds;

    public TooManyLoginAttemptsException(Duration remaining) {
        super(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_LOGIN_ATTEMPTS", "Too many incorrect sign-in attempts. Try again in "
            + minutesRoundedUp(remaining) + (minutesRoundedUp(remaining) == 1 ? " minute." : " minutes."));
        this.retryAfterSeconds = Math.max(1, secondsRoundedUp(remaining));
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    @Override
    public HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        return headers;
    }

    private static long minutesRoundedUp(Duration remaining) {
        long minutes = remaining.toMinutes();
        return remaining.equals(Duration.ofMinutes(minutes)) ? minutes : minutes + 1;
    }

    private static long secondsRoundedUp(Duration remaining) {
        long seconds = remaining.toSeconds();
        return remaining.equals(Duration.ofSeconds(seconds)) ? seconds : seconds + 1;
    }
}
