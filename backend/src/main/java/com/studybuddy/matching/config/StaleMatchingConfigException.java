package com.studybuddy.matching.config;

import com.studybuddy.common.error.ApiException;
import org.springframework.http.HttpStatus;

/** Prevents one administrator silently replacing another administrator's newer settings. */
public class StaleMatchingConfigException extends ApiException {
    public StaleMatchingConfigException() {
        super(HttpStatus.CONFLICT, "STALE_MATCHING_CONFIG", "Matching settings changed; reload them before saving");
    }
}
