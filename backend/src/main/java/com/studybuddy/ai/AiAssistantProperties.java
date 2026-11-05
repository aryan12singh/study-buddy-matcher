package com.studybuddy.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.ZoneId;

/**
 * AI study assistant settings, bound from {@code app.ai.assistant.*}. Separate from E1's
 * {@code app.ai.explanation.*} so the two features use different keys and never share a quota. The
 * API key comes only from the git-ignored {@code application-local.yml}; without it the assistant
 * stays off.
 */
@Validated
@ConfigurationProperties("app.ai.assistant")
public record AiAssistantProperties(
        boolean enabled,
        @NotBlank String baseUrl,
        @NotBlank String model,
        String apiKey,
        @NotNull Duration timeout,
        @Positive int maxOutputTokens,
        @PositiveOrZero int dailyCallLimit,
        @PositiveOrZero int dailyCallLimitPerStudent,
        @NotBlank String quotaZone,
        @NotNull Duration rateLimitCooldown) {

    public AiAssistantProperties {
        // A zero timeout makes the HTTP client fail at startup with an unclear error.
        if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
            throw new IllegalArgumentException("app.ai.assistant.timeout must be positive");
        }
    }

    /** True only when the assistant is switched on and a key is configured. */
    public boolean ready() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /** How a Gemini client built from these settings connects; the key stays in the settings. */
    public GeminiSettings geminiSettings(String purpose, boolean turnOffThinking) {
        return new GeminiSettings(purpose, enabled, baseUrl, model, apiKey, timeout, maxOutputTokens, turnOffThinking);
    }

    /** A new call allowance with these limits, counted separately from every other client's. */
    public AiQuota newQuota() {
        return new AiQuota(dailyCallLimit, dailyCallLimitPerStudent, rateLimitCooldown, ZoneId.of(quotaZone));
    }

    /** Never print the key, even in a startup error. */
    @Override
    public String toString() {
        return "AiAssistantProperties[enabled=" + enabled + ", model=" + model + ", apiKey="
                + (apiKey == null ? "absent" : "set") + "]";
    }
}
