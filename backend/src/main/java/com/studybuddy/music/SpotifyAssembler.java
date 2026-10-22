package com.studybuddy.music;

import org.springframework.stereotype.Component;

@Component
public class SpotifyAssembler {
    public SpotifyPlaylistDto toDto(SpotifyPlaylist playlist) {
        return new SpotifyPlaylistDto(playlist.id(), playlist.uri(), playlist.name(), playlist.owner(), playlist.imageUrl());
    }

    public SpotifyAccessTokenDto toDto(SpotifyTokens tokens) {
        return new SpotifyAccessTokenDto(tokens.accessToken(), tokens.expiresAt());
    }
}
