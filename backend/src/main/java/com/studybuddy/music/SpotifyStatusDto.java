package com.studybuddy.music;

/** {@code configured} is false when this server has no Spotify credentials at all. */
public record SpotifyStatusDto(boolean configured, boolean connected) {
}
