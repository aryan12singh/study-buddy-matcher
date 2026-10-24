package com.studybuddy.music;

/**
 * {@code configured} is false when this server has no Spotify credentials at all.
 * {@code embedPlaylistId} is the playlist offered to students without Premium; it needs no credentials.
 */
public record SpotifyStatusDto(boolean configured, boolean connected, String embedPlaylistId) {
}
