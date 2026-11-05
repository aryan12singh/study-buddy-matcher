package com.studybuddy.music;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers which student started each Spotify sign-in. The random, single-use {@code state} ties
 * Spotify's redirect to that sign-in, and finishing it also needs the same student signed in, so a
 * sign-in link sent to someone else can never connect their Spotify account to the sender.
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

    /** Whether this sign-in is known and not expired, without using it up. */
    public boolean isPending(String state) {
        Pending entry = state == null ? null : pending.get(state);
        return entry != null && !entry.expiresAt().isBefore(clock.instant());
    }

    /** Uses up the sign-in; true only if it is not expired and {@code studentId} started it. */
    public boolean finish(String state, Long studentId) {
        Pending entry = state == null ? null : pending.remove(state);
        return entry != null && !entry.expiresAt().isBefore(clock.instant()) && entry.studentId().equals(studentId);
    }
}
