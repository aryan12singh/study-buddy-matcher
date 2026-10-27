package com.studybuddy.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Chat assistant settings, bound from {@code app.ai.assistant.chat.*}. Reading a message is a small
 * job, so the assistant uses its own, lighter model with its own daily allowance: chatting never
 * spends the allowance students need for quizzes and study plans. It shares the assistant's API
 * key, base URL, on/off switch and rate-limit pause.
 *
 * @param maxMessageCharacters the longest message a student may send
 * @param matchesShown how many buddy matches a reply shows; the rest are on the Find buddies page
 * @param timeZone the calendar the AI uses for "today", to work out dates such as "20 Nov"
 * @param model the Gemini model that reads messages
 * @param dailyCallLimit messages all students together may send to the AI per day
 * @param dailyCallLimitPerStudent messages one student may send to the AI per day
 */
@Validated
@ConfigurationProperties("app.ai.assistant.chat")
public record AssistantProperties(
        @Positive int maxMessageCharacters,
        @Positive int matchesShown,
        @NotBlank String timeZone,
        @NotBlank String model,
        @Positive int maxOutputTokens,
        @NotNull Duration timeout,
        @PositiveOrZero int dailyCallLimit,
        @PositiveOrZero int dailyCallLimitPerStudent) {

    public AssistantProperties {
        if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
            throw new IllegalArgumentException("app.ai.assistant.chat.timeout must be positive");
        }
    }
}
