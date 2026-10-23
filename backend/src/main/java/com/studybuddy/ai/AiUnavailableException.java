package com.studybuddy.ai;

import com.studybuddy.common.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * The assistant could not produce usable AI output: switched off, over quota, rate limited, timed
 * out, or the reply was rejected. Returned as 503 {@code AI_UNAVAILABLE} so the screen can offer
 * the manual path. The message never contains the prompt, the reply or the provider's error.
 */
public class AiUnavailableException extends ApiException {
    public AiUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE",
                "The AI assistant is unavailable right now; try again later or continue without it");
    }
}
