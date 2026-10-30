package com.studybuddy.music;

import org.springframework.stereotype.Component;

@Component
public class SpotifyAssembler {
    public SpotifyAccessTokenDto toDto(SpotifyTokens tokens) {
        return new SpotifyAccessTokenDto(tokens.accessToken(), tokens.expiresAt());
    }
}
