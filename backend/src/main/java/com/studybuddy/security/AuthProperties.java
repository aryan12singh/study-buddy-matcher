package com.studybuddy.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Sign-in settings bound from {@code app.auth}: the JWT signing secret (at least 32 characters),
 * the token lifetime and the origins allowed by CORS. The secret comes from the environment or
 * {@code application-local.yml}, never from a tracked file.
 */
@Validated
@ConfigurationProperties("app.auth")
public record AuthProperties(
        @NotBlank @Size(min = 32) String secret,
        @NotNull Duration tokenLifetime,
        @NotEmpty List<String> allowedOrigins) {
    public AuthProperties {
        if (tokenLifetime != null && (tokenLifetime.isZero() || tokenLifetime.isNegative())) {
            throw new IllegalArgumentException("Token lifetime must be positive");
        }
    }
}
