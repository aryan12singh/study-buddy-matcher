package com.studybuddy.music;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Each connected student's Spotify tokens, kept in memory only. Tokens are never written to the
 * database, so a server restart simply asks students to connect Spotify again.
 */
@Component
public class SpotifyTokenStore {
    private final Map<Long, SpotifyTokens> tokens = new ConcurrentHashMap<>();

    public Optional<SpotifyTokens> find(Long studentId) {
        return Optional.ofNullable(tokens.get(studentId));
    }

    public void save(Long studentId, SpotifyTokens studentTokens) {
        tokens.put(studentId, studentTokens);
    }

    public void remove(Long studentId) {
        tokens.remove(studentId);
    }
}
