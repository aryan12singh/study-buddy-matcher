package com.studybuddy.studyroom;

import com.studybuddy.common.error.ApiException;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/** A student reacted too often in one burst window; the response says when the next one will go through. */
public class ReactionRateLimitException extends ApiException {
    private final long retryAfterSeconds;

    public ReactionRateLimitException(Duration remaining) {
        super(HttpStatus.TOO_MANY_REQUESTS, "REACTION_RATE_LIMITED", "You are reacting too quickly; wait a moment and try again");
        long seconds = remaining.toSeconds();
        this.retryAfterSeconds = Math.max(1, remaining.equals(Duration.ofSeconds(seconds)) ? seconds : seconds + 1);
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
}
