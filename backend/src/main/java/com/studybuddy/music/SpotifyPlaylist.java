package com.studybuddy.music;

/** A playlist found by a Spotify search. */
public record SpotifyPlaylist(String id, String uri, String name, String owner, String imageUrl) {
}
