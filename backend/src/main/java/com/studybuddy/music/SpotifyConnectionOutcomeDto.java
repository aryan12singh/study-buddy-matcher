package com.studybuddy.music;

/** How finishing a Spotify sign-in went: {@code connected}, {@code expired} or {@code failed}. */
public record SpotifyConnectionOutcomeDto(String outcome) {
}
