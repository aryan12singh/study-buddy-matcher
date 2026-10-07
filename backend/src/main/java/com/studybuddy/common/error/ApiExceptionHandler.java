package com.studybuddy.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private final HttpServletRequest request;

    public ApiExceptionHandler(HttpServletRequest request) {
        this.request = request;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiProblem> api(ApiException error) {
        return ResponseEntity.status(error.getStatus()).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(ApiProblem.of(error.getStatus(), error.getCode(), error.getMessage(), request.getRequestURI(), error.getFieldErrors()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiProblem> conflict(RuntimeException error) {
        return problem(HttpStatus.CONFLICT, "STATE_CONFLICT", error.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiProblem> integrity(DataIntegrityViolationException error) {
        return problem(HttpStatus.CONFLICT, "DATA_CONFLICT", "The action conflicts with saved data; refresh and try again");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiProblem> validation(MethodArgumentNotValidException error) {
        var fields = new LinkedHashMap<String, String>();
        error.getBindingResult().getFieldErrors().forEach(field -> fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(ApiProblem.validation(fields, request.getRequestURI()));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, ConstraintViolationException.class
    })
    public ResponseEntity<ApiProblem> malformed(Exception error) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "The request contains invalid fields or values");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiProblem> unexpected(Exception error) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "The action could not be completed; try again later");
    }

    private ResponseEntity<ApiProblem> problem(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(ApiProblem.of(status, code, message, request.getRequestURI()));
    }
}
