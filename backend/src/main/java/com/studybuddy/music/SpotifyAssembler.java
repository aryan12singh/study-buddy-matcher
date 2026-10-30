package com.studybuddy.music;

import org.springframework.stereotype.Component;

/**
 * Builds the {@link SpotifyAccessTokenDto} the Study Room player uses. Only the short-lived access
 * token is exposed; the refresh token stays on the server.
 */
@Component
public class SpotifyAssembler {
    public SpotifyAccessTokenDto toDto(SpotifyTokens tokens) {
        return new SpotifyAccessTokenDto(tokens.accessToken(), tokens.expiresAt());
    }
}
