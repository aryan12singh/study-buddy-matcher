package com.studybuddy.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.studybuddy.common.error.ApiProblem;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class SecurityProblemWriter implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final JsonMapper json;

    public SecurityProblemWriter(JsonMapper json) {
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException error)
    throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Sign in with an active account to continue");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException error)
    throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this action");
    }

    /** The same problem shape as ApiExceptionHandler, serialized by the application's JSON mapper. */
    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code,
        String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(json.writeValueAsString(ApiProblem.of(status, code, message, request.getRequestURI())));
    }
}
