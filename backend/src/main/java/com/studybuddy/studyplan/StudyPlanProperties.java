package com.studybuddy.studyplan;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/**
 * Study roadmap limits, bound from {@code app.ai.assistant.study-plan.*}. Spring's multipart limit
 * reads the same upload size, so the two cannot drift apart. The roadmap limits are given to
 * Gemini in the prompt and enforced again on its reply.
 */
@Validated
@ConfigurationProperties("app.ai.assistant.study-plan")
public record StudyPlanProperties(
        @NotNull DataSize maxUploadSize,
        @Positive int maxTopicsCharacters,
        @Positive int maxTextFileCharacters,
        @Positive int maxSourceTopics,
        @Positive int maxEntriesPerBlock,
        @Positive int maxTasksPerEntry) {
}
