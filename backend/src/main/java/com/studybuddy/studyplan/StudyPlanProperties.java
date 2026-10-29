package com.studybuddy.studyplan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/**
 * Study roadmap limits, bound from {@code app.ai.assistant.study-plan.*}. Spring's multipart limit
 * reads the same upload size, so the two cannot drift apart. The roadmap limits are given to
 * Gemini in the prompt and enforced again on its reply.
 *
 * @param maxUploadSize the most all of one plan's files may add up to
 * @param maxTextFileCharacters the most text all of one plan's text files may hold together
 * @param maxWeeks the most weeks a personal plan may cover, which bounds how far away the exam can be
 * @param maxSessionMeetings the most upcoming meetings of a weekly session that one session plan covers
 * @param timeZone the zone whose calendar decides "today" for a plan's first week
 */
@Validated
@ConfigurationProperties("app.ai.assistant.study-plan")
public record StudyPlanProperties(
        @NotNull DataSize maxUploadSize,
        @Positive int maxFiles,
        @Positive int maxTopicsCharacters,
        @Positive int maxTextFileCharacters,
        @Positive int maxSourceTopics,
        @Positive int maxEntriesPerBlock,
        @Positive int maxTasksPerEntry,
        @Positive int maxWeeks,
        @Positive int maxHoursPerWeek,
        @Positive int maxSessionMeetings,
        @NotBlank String timeZone) {
}
