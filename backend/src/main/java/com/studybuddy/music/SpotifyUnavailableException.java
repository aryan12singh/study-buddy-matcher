package com.studybuddy.music;

import com.studybuddy.common.error.ApiException;
import org.springframework.http.HttpStatus;

/** Spotify is not configured on this server, or did not answer. */
public class SpotifyUnavailableException extends ApiException {
    public SpotifyUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "SPOTIFY_UNAVAILABLE", message);
    }
}
