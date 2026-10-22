package com.studybuddy.music;

import java.time.Duration;
import java.time.Instant;

/** A student's Spotify access token, the refresh token that renews it, and when it expires. */
public record SpotifyTokens(String accessToken, String refreshToken, Instant expiresAt) {

    /** Treats a token as expired slightly early, so it never lapses mid-request. */
    public boolean expiresWithin(Duration margin, Instant now) {
        return !now.plus(margin).isBefore(expiresAt);
    }

    /** Spotify may omit the refresh token when renewing; the old one then stays valid. */
    public SpotifyTokens renewedWith(SpotifyTokens renewed) {
        String refresh = renewed.refreshToken() == null || renewed.refreshToken().isBlank() ? refreshToken : renewed.refreshToken();
        return new SpotifyTokens(renewed.accessToken(), refresh, renewed.expiresAt());
    }

    /** Never print the tokens. */
    @Override
    public String toString() {
        return "SpotifyTokens[expiresAt=" + expiresAt + "]";
    }
}
