package com.studybuddy.matchexplanation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * AI match explanation settings, bound from {@code app.ai.explanation.*}. The API key comes only
 * from the git-ignored {@code application-local.yml}; without it the feature stays off.
 */
@Validated
@ConfigurationProperties("app.ai.explanation")
public record AiExplanationProperties(
        boolean enabled,
        @NotBlank String baseUrl,
        @NotBlank String model,
        String apiKey,
        @NotNull Duration timeout,
        @Positive int maxOutputTokens,
        @Positive int maxCharacters,
        @PositiveOrZero int dailyCallLimit,
        @NotBlank String quotaZone) {

    /** True only when the feature is switched on and a key is configured. */
    public boolean ready() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /** Never print the key, even in a startup error. */
    @Override
    public String toString() {
        return "AiExplanationProperties[enabled=" + enabled + ", model=" + model + ", apiKey="
                + (apiKey == null ? "absent" : "set") + "]";
    }
}
