package com.studybuddy.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
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

    /** Spring refuses an over-limit upload before any controller runs, so it is mapped here. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiProblem> uploadTooLarge(MaxUploadSizeExceededException error) {
        return problem(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE", "The file is larger than the upload limit");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiProblem> unexpected(Exception error) {
        // The stack trace finds the fault; the message is left out because it can carry SQL or user data.
        var trace = new RuntimeException(error.getClass().getName());
        trace.setStackTrace(error.getStackTrace());
        LOG.error("Unexpected failure on {} {}", request.getMethod(), request.getRequestURI(), trace);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "The action could not be completed; try again later");
    }

    private ResponseEntity<ApiProblem> problem(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(ApiProblem.of(status, code, message, request.getRequestURI()));
    }
}
