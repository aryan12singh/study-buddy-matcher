package com.studybuddy.quiz;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Group quiz settings, bound from {@code app.ai.assistant.quiz.*}. The question count is given to
 * Gemini in the prompt and enforced again on its reply.
 *
 * @param questionCount exactly how many questions every quiz has
 * @param openDuration how long a quiz stays open before it closes by itself
 * @param maxFocusCharacters the longest focus a student may give; it also titles the quiz
 */
@Validated
@ConfigurationProperties("app.ai.assistant.quiz")
public record QuizProperties(
        @Positive int questionCount,
        @NotNull Duration openDuration,
        @Positive int maxFocusCharacters) {

    public QuizProperties {
        if (openDuration != null && (openDuration.isZero() || openDuration.isNegative())) {
            throw new IllegalArgumentException("app.ai.assistant.quiz.open-duration must be positive");
        }
        if (maxFocusCharacters > Quiz.TITLE_LIMIT) {
            throw new IllegalArgumentException("app.ai.assistant.quiz.max-focus-characters must fit a quiz title of "
                    + Quiz.TITLE_LIMIT + " characters");
        }
    }
}
