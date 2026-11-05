package com.studybuddy.music;

import jakarta.validation.constraints.NotBlank;

/** What Spotify's redirect handed to the browser that completed the sign-in. */
public record SpotifyAuthorizationInput(@NotBlank String state, @NotBlank String code) {
}
