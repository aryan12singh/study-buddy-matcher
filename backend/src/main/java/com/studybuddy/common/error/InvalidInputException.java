package com.studybuddy.common.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Thrown when input passes bean validation but breaks a business rule. Maps to 400 Bad Request,
 * optionally naming the field that is wrong so the form can show the message beside it.
 */
public class InvalidInputException extends ApiException {

    public InvalidInputException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_INPUT", message);
    }

    public InvalidInputException(String field, String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_INPUT", message, Map.of(field, message));
    }
}
