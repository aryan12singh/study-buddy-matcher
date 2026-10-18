package com.studybuddy.matchexplanation;

import com.studybuddy.common.error.ApiException;
import org.springframework.http.HttpStatus;

/** The current scoring inputs no longer describe the card that requested an explanation. */
public class StaleMatchEvaluationException extends ApiException {
    public StaleMatchEvaluationException() {
        super(HttpStatus.CONFLICT, "STALE_MATCH_EVALUATION", "This match changed; refresh matches before explaining it");
    }
}
