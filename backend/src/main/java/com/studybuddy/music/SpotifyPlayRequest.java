package com.studybuddy.music;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Which playlist to start, and on which player (the one in the student's browser tab). */
public record SpotifyPlayRequest(
        @NotBlank @Size(max = 100) String deviceId,
        @NotBlank @Pattern(regexp = "spotify:playlist:[A-Za-z0-9]+", message = "Choose a Spotify playlist") String playlistUri) {
}
