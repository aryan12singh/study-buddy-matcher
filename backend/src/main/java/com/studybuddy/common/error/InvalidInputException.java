package com.studybuddy.common.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class InvalidInputException extends ApiException {

    public InvalidInputException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_INPUT", message);
    }

    public InvalidInputException(String field, String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_INPUT", message, Map.of(field, message));
    }
}
