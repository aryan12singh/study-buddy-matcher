package com.studybuddy.music;

import com.studybuddy.common.error.ApiException;
import org.springframework.http.HttpStatus;

/** Spotify refused to start playback, for a reason the student can act on. */
public class SpotifyPlaybackException extends ApiException {
    public SpotifyPlaybackException(String message) {
        super(HttpStatus.CONFLICT, "SPOTIFY_PLAYBACK_REFUSED", message);
    }
}
