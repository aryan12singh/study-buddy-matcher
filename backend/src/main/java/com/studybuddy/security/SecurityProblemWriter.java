package com.studybuddy.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class SecurityProblemWriter implements AuthenticationEntryPoint, AccessDeniedHandler {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException error)
    throws IOException {
        write(request, response, 401, "Unauthorized", "UNAUTHENTICATED", "Sign in with an active account to continue");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException error)
    throws IOException {
        write(request, response, 403, "Forbidden", "FORBIDDEN", "You do not have permission to perform this action");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, int status, String title,
        String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setHeader("Cache-Control", "no-store");
        String instance = request.getRequestURI().replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status
 + ",\"detail\":\"" + message + "\",\"instance\":\"" + instance + "\",\"code\":\"" + code
 + "\",\"message\":\"" + message + "\",\"fieldErrors\":{},\"timestamp\":\"" + Instant.now() + "\"}");
    }
}
