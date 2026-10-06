package com.studybuddy.common.error;

import com.studybuddy.admin.AdminActionNotAllowedException;
import com.studybuddy.admin.DuplicateEmailException;
import com.studybuddy.admin.InvalidAdminUserException;
import com.studybuddy.admin.UserNotFoundException;
import com.studybuddy.connection.ConnectionNotFoundException;
import com.studybuddy.connection.NotConnectionParticipantException;
import com.studybuddy.matchrequest.MatchRequestNotAllowedException;
import com.studybuddy.matchrequest.MatchRequestNotFoundException;
import com.studybuddy.matchrequest.StudentNotFoundException;
import com.studybuddy.notification.NotificationNotAllowedException;
import com.studybuddy.notification.NotificationNotFoundException;
import com.studybuddy.studygroup.CourseNotFoundException;
import com.studybuddy.studygroup.GroupJoinRequestNotFoundException;
import com.studybuddy.studygroup.InvalidStudyGroupException;
import com.studybuddy.studygroup.NotGroupLeaderException;
import com.studybuddy.studygroup.StudyGroupActionNotAllowedException;
import com.studybuddy.studygroup.StudyGroupNotFoundException;
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

    @ExceptionHandler({
            UserNotFoundException.class, StudentNotFoundException.class, MatchRequestNotFoundException.class,
            ConnectionNotFoundException.class, StudyGroupNotFoundException.class, GroupJoinRequestNotFoundException.class,
            CourseNotFoundException.class, NotificationNotFoundException.class
    })
    public ResponseEntity<ApiProblem> missing(RuntimeException error) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", error.getMessage());
    }

    @ExceptionHandler({
            NotGroupLeaderException.class, NotConnectionParticipantException.class, NotificationNotAllowedException.class
    })
    public ResponseEntity<ApiProblem> forbidden(RuntimeException error) {
        return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", error.getMessage());
    }

    @ExceptionHandler({
            MatchRequestNotAllowedException.class, StudyGroupActionNotAllowedException.class,
            AdminActionNotAllowedException.class, IllegalStateException.class
    })
    public ResponseEntity<ApiProblem> conflict(RuntimeException error) {
        return problem(HttpStatus.CONFLICT, "STATE_CONFLICT", error.getMessage());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiProblem> email(DuplicateEmailException error) {
        return problem(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "This email is already registered");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiProblem> integrity(DataIntegrityViolationException error) {
        return problem(HttpStatus.CONFLICT, "DATA_CONFLICT", "The action conflicts with saved data; refresh and try again");
    }

    @ExceptionHandler({
            InvalidStudyGroupException.class, InvalidAdminUserException.class
    })
    public ResponseEntity<ApiProblem> invalid(RuntimeException error) {
        if (error instanceof ApiException apiError) {
            return api(apiError);
        }
        return problem(HttpStatus.BAD_REQUEST, "INVALID_INPUT", error.getMessage());
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
