package com.studybuddy.common.error;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler(new MockHttpServletRequest("GET", "/api/example"));

    @Test
    void aDomainStateConflictIsStill409() {
        ResponseEntity<ApiProblem> reply = handler.conflict(new IllegalStateException("Connection 4 has already ended"));

        assertEquals(HttpStatus.CONFLICT, reply.getStatusCode());
        assertEquals("STATE_CONFLICT", reply.getBody().code());
        assertEquals("Connection 4 has already ended", reply.getBody().message());
    }

    @Test
    void aBrokenInternalRuleIsAServerErrorWithoutItsMessage() {
        ResponseEntity<ApiProblem> reply = handler.unexpected(new InternalInvariantException("A database mutation must run in a transaction"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, reply.getStatusCode());
        assertEquals("INTERNAL_ERROR", reply.getBody().code());
        assertTrue(!reply.getBody().message().contains("transaction"));
    }

    @Test
    void springRefusalsKeepTheirOwnStatus() {
        assertEquals(HttpStatus.NOT_FOUND, handler.unexpected(new NoResourceFoundException(HttpMethod.GET, "/api/nothing", "nothing")).getStatusCode());
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                handler.unexpected(new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON))).getStatusCode());

        ResponseEntity<ApiProblem> notAllowed = handler.unexpected(new HttpRequestMethodNotSupportedException("PATCH", List.of("GET", "PUT")));
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, notAllowed.getStatusCode());
        assertEquals("METHOD_NOT_ALLOWED", notAllowed.getBody().code());
        assertTrue(notAllowed.getHeaders().getAllow().contains(HttpMethod.GET));
    }

    @Test
    void notAcceptableHasNoBodyTheClientCouldNotReadAnyway() {
        ResponseEntity<ApiProblem> reply = handler.unexpected(new HttpMediaTypeNotAcceptableException(List.of(MediaType.APPLICATION_JSON)));

        assertEquals(HttpStatus.NOT_ACCEPTABLE, reply.getStatusCode());
        assertNull(reply.getBody());
    }

    @Test
    void aFormValueOfTheWrongTypeIsReportedWithoutJavaClassNames() throws NoSuchMethodException {
        var binding = new BeanPropertyBindingResult(new Object(), "input");
        binding.addError(new FieldError("input", "examDate", "garbage", true, null, null,
                "Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'"));
        binding.addError(new FieldError("input", "courseId", "Choose a course"));
        var parameter = new MethodParameter(getClass().getDeclaredMethod("formEndpoint", Object.class), 0);

        ResponseEntity<ApiProblem> reply = handler.validation(new MethodArgumentNotValidException(parameter, binding));

        assertEquals(HttpStatus.BAD_REQUEST, reply.getStatusCode());
        assertEquals("VALIDATION_FAILED", reply.getBody().code());
        assertEquals("Enter a valid value", reply.getBody().fieldErrors().get("examDate"));
        assertEquals("Choose a course", reply.getBody().fieldErrors().get("courseId"));
    }

    /** Stands in for a controller method; the exception only needs a parameter to point at. */
    @SuppressWarnings("unused")
    private void formEndpoint(Object input) { }
}
