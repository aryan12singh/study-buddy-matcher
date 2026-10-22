package com.studybuddy.music;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers which student started each Spotify sign-in. Spotify's redirect carries no app token,
 * so the random, single-use {@code state} value is what ties the callback back to the student and
 * stops another site from completing a sign-in on their behalf.
 */
@Component
public class SpotifyAuthorizationStore {
    private static final int STATE_BYTES = 32;

    private record Pending(Long studentId, Instant expiresAt) { }

    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final SpotifyProperties properties;
    private final Clock clock;

    public SpotifyAuthorizationStore(SpotifyProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String start(Long studentId) {
        Instant now = clock.instant();
        pending.values().removeIf(entry -> entry.expiresAt().isBefore(now));
        byte[] bytes = new byte[STATE_BYTES];
        random.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        pending.put(state, new Pending(studentId, now.plus(properties.authorizationLifetime())));
        return state;
    }

    /** The student who started this sign-in, if it is known and not expired. Each state works once. */
    public Optional<Long> finish(String state) {
        Pending entry = state == null ? null : pending.remove(state);
        if (entry == null || entry.expiresAt().isBefore(clock.instant())) {
            return Optional.empty();
        }
        return Optional.of(entry.studentId());
    }
}
