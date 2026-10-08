package com.studybuddy.common.error;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;

/** RFC 9457 fields plus the stable application and form-validation fields. */
public record ApiProblem(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        String message,
        Map<String, String> fieldErrors,
        Instant timestamp) {

    public static ApiProblem of(HttpStatus status, String code, String message, String instance) {
        return of(status, code, message, instance, Map.of());
    }

    public static ApiProblem of(HttpStatus status, String code, String message, String instance, Map<String, String> fields) {
        return new ApiProblem("about:blank", status.getReasonPhrase(), status.value(), message, instance,
            code, message, Map.copyOf(fields), Instant.now());
    }

    public static ApiProblem validation(Map<String, String> fields, String instance) {
        String message = "Check the highlighted fields";
        return new ApiProblem("about:blank", "Bad Request", 400, message, instance,
            "VALIDATION_FAILED", message, Map.copyOf(fields), Instant.now());
    }
}
