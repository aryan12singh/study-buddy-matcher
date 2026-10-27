package com.studybuddy.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Chat assistant settings, bound from {@code app.ai.assistant.chat.*}.
 *
 * @param maxMessageCharacters the longest message a student may send
 * @param matchesShown how many buddy matches a reply shows; the rest are on the Find buddies page
 * @param timeZone the calendar the AI uses for "today", to work out dates such as "20 Nov"
 */
@Validated
@ConfigurationProperties("app.ai.assistant.chat")
public record AssistantProperties(
        @Positive int maxMessageCharacters,
        @Positive int matchesShown,
        @NotBlank String timeZone) {
}
