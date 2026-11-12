package com.studybuddy.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.ZoneId;

/**
 * Settings for Claude, the second AI provider, bound from {@code app.ai.claude.*}. Claude is asked
 * only when this is switched on with a key: after a failed Gemini call, or first when
 * {@code app.ai.primary-provider} is {@code claude}. The API key comes only from the git-ignored
 * {@code application-local.yml}; without it Claude stays off and nothing changes.
 *
 * @param effort how much reasoning Claude spends on a reply: low, medium, high, xhigh or max
 * @param maxTokens reply size, reasoning included, for a match explanation or a Pixel message
 * @param studyToolsMaxTokens reply size, reasoning included, for a whole quiz or study plan
 * @param dailyCallLimit Claude calls all students together may cause per day; Claude is paid per call
 */
@Validated
@ConfigurationProperties("app.ai.claude")
public record ClaudeProperties(
        boolean enabled,
        @NotBlank String baseUrl,
        String apiKey,
        @NotBlank String model,
        @NotBlank @Pattern(regexp = "low|medium|high|xhigh|max") String effort,
        @Positive int maxTokens,
        @Positive int studyToolsMaxTokens,
        @NotNull Duration timeout,
        @PositiveOrZero int dailyCallLimit,
        @PositiveOrZero int dailyCallLimitPerStudent,
        @NotBlank String quotaZone,
        @NotNull Duration rateLimitCooldown) {

    public ClaudeProperties {
        // A zero timeout makes the HTTP client fail at startup with an unclear error.
        if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
            throw new IllegalArgumentException("app.ai.claude.timeout must be positive");
        }
    }

    /** True only when Claude is switched on and a key is configured. */
    public boolean ready() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /** One call allowance for every feature's Claude calls, since they share this key. */
    public AiQuota newQuota() {
        return new AiQuota(dailyCallLimit, dailyCallLimitPerStudent, rateLimitCooldown, ZoneId.of(quotaZone));
    }

    /** Never print the key, even in a startup error. */
    @Override
    public String toString() {
        return "ClaudeProperties[enabled=" + enabled + ", model=" + model + ", apiKey="
                + (apiKey == null ? "absent" : "set") + "]";
    }
}
