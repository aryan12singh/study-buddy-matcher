package com.studybuddy.security;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Sign-in throttling settings, bound from {@code app.auth.login-throttle.*}. */
@Validated
@ConfigurationProperties("app.auth.login-throttle")
public record LoginThrottleProperties(@Positive int maxFailures, @NotNull Duration lockout) {
    public LoginThrottleProperties {
        if (lockout != null && (lockout.isZero() || lockout.isNegative())) {
            throw new IllegalArgumentException("app.auth.login-throttle.lockout must be positive");
        }
    }
}
