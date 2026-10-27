package com.studybuddy.music;

import java.time.Instant;

/** A short-lived token for Spotify's in-browser player. The refresh token never leaves the server. */
public record SpotifyAccessTokenDto(String accessToken, Instant expiresAt) {
}
