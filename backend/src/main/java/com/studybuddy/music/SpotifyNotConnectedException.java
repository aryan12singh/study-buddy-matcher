package com.studybuddy.music;

import com.studybuddy.common.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * The student has not connected Spotify, or the connection lapsed. A 409 rather than a 401, because
 * a 401 tells the app the student's own sign-in has expired and logs them out.
 */
public class SpotifyNotConnectedException extends ApiException {
    public SpotifyNotConnectedException(String message) {
        super(HttpStatus.CONFLICT, "SPOTIFY_NOT_CONNECTED", message);
    }
}
