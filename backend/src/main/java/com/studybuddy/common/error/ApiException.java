package com.studybuddy.common.error;

import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * Base class for every expected API failure. It carries the HTTP status, a stable machine-readable
 * code and optional per-field messages, which {@link ApiExceptionHandler} turns into a problem
 * response.
 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Map<String, String> fieldErrors;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, Map.of());
    }

    public ApiException(HttpStatus status, String code, String message, Map<String, String> fieldErrors) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    /** Extra response headers; none unless a subclass needs one, such as {@code Retry-After}. */
    public HttpHeaders getHeaders() {
        return new HttpHeaders();
    }
}
