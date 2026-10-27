package com.studybuddy.music;

import com.studybuddy.common.error.ApiException;
import org.springframework.http.HttpStatus;

/** The sound provider is not configured, did not answer, or had nothing to play. */
public class AmbientUnavailableException extends ApiException {
    public AmbientUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "AMBIENT_UNAVAILABLE", message);
    }
}
