package com.studybuddy.music;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Ambient sound settings, bound from {@code app.ambient.*}. The Freesound API key comes only from
 * the git-ignored {@code application-local.yml}; without it sounds report as unavailable.
 */
@Validated
@ConfigurationProperties("app.ambient")
public record AmbientProperties(
        @NotBlank String baseUrl,
        String apiKey,
        @NotNull Duration timeout,
        @Positive int candidateCount,
        @Positive int minDurationSeconds,
        @Positive int maxDurationSeconds,
        @NotEmpty List<@Valid AmbientCategory> categories) {

    public AmbientProperties {
        // A zero timeout makes the HTTP client fail at startup with an unclear error.
        if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
            throw new IllegalArgumentException("app.ambient.timeout must be positive");
        }
        if (minDurationSeconds > maxDurationSeconds) {
            throw new IllegalArgumentException("app.ambient.min-duration-seconds must not exceed max-duration-seconds");
        }
        categories = categories == null ? List.of() : List.copyOf(categories);
    }

    /** True only when a key is configured. */
    public boolean ready() {
        return apiKey != null && !apiKey.isBlank();
    }

    public Optional<AmbientCategory> category(String id) {
        return categories.stream().filter(category -> category.id().equals(id)).findFirst();
    }

    /** Never print the key, even in a startup error. */
    @Override
    public String toString() {
        return "AmbientProperties[baseUrl=" + baseUrl + ", apiKey=" + (apiKey == null ? "absent" : "set")
                + ", categories=" + categories.size() + "]";
    }
}
