package com.studybuddy.common.http;

import com.studybuddy.common.error.ApiProblem;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Refuses a JSON request body over {@code app.http.max-json-body-size} with 413 before Jackson
 * reads it, so one oversized request cannot hold a large document in memory. A body sent without
 * a length (chunked) is read up to one byte past the limit, which is all that is needed to decide.
 * Registered as a plain servlet filter, so it runs after the security chain has added CORS headers.
 */
@Component
public class JsonBodySizeFilter extends OncePerRequestFilter {
    private final long maxBytes;
    private final JsonMapper json;

    public JsonBodySizeFilter(HttpProperties properties, JsonMapper json) {
        this.maxBytes = properties.maxJsonBodySize().toBytes();
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return true;
        }
        try {
            MediaType type = MediaType.parseMediaType(contentType);
            return !type.isCompatibleWith(MediaType.APPLICATION_JSON) && !type.getSubtype().endsWith("+json");
        } catch (InvalidMediaTypeException unreadable) {
            // Spring MVC answers an unreadable content type with 415 itself.
            return true;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        long declared = request.getContentLengthLong();
        if (declared > maxBytes) {
            reject(request, response);
            return;
        }
        if (declared >= 0) {
            // The servlet container never reads past the declared length.
            chain.doFilter(request, response);
            return;
        }
        byte[] body = request.getInputStream().readNBytes(Math.toIntExact(maxBytes + 1));
        if (body.length > maxBytes) {
            reject(request, response);
            return;
        }
        chain.doFilter(new BufferedBodyRequest(request, body), response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpStatus status = HttpStatus.CONTENT_TOO_LARGE;
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(json.writeValueAsString(ApiProblem.of(status, "REQUEST_TOO_LARGE",
            "The request is larger than the " + maxBytes / 1024 + " KB limit", request.getRequestURI())));
    }
}
