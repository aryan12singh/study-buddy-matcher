package com.studybuddy.assistant;

import com.studybuddy.common.InputRules;
import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Turns the AI's reading of a message into an {@link AssistantRequest}, or rejects it. The reply
 * must be a JSON object naming a known intent. Each detail is kept only if it is the right kind of
 * value: an unknown enum value, a number out of range or an unreadable date is dropped rather than
 * guessed, so a slightly wrong detail never blocks an otherwise useful answer.
 */
@Component
public class AssistantOutputValidator {
    /** A week has 168 hours; a meeting longer than a day is not something the app schedules. */
    private static final int MAX_WEEKLY_HOURS = 168;
    private static final int MAX_MEETING_MINUTES = 24 * 60;

    private final JsonMapper json;

    public AssistantOutputValidator(JsonMapper json) {
        this.json = json;
    }

    public Optional<AssistantRequest> validate(String raw) {
        JsonNode reply;
        try {
            reply = json.readTree(raw == null ? "" : raw);
        } catch (JacksonException notJson) {
            return Optional.empty();
        }
        Optional<AssistantIntentType> intent = enumOf(reply.path("intent"), AssistantIntentType.class);
        if (!reply.isObject() || intent.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AssistantRequest(
                intent.get(),
                textOf(reply.path("courseCode")),
                enumOf(reply.path("studyGoal"), StudyGoal.class).orElse(null),
                enumOf(reply.path("studyMode"), StudyMode.class).orElse(null),
                enumOf(reply.path("groupSize"), GroupSizePreference.class).orElse(null),
                wholeNumberOf(reply.path("minSharedHours"), 1, MAX_WEEKLY_HOURS),
                enumOf(reply.path("timeOfDay"), TimeOfDay.class).orElse(null),
                textOf(reply.path("groupName")),
                wholeNumberOf(reply.path("durationMinutes"), 1, MAX_MEETING_MINUTES),
                dateOf(reply.path("examDate")),
                wholeNumberOf(reply.path("hoursPerWeek"), 1, MAX_WEEKLY_HOURS),
                textOf(reply.path("topic")),
                enumOf(reply.path("page"), AppPage.class).orElse(null)));
    }

    private static <E extends Enum<E>> Optional<E> enumOf(JsonNode node, Class<E> type) {
        if (!node.isString()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Enum.valueOf(type, node.asString().strip()));
        } catch (IllegalArgumentException unknown) {
            return Optional.empty();
        }
    }

    /** A non-blank string that fits a column once trimmed. */
    private static String textOf(JsonNode node) {
        if (!node.isString()) {
            return null;
        }
        String text = node.asString().strip();
        return text.isEmpty() || text.length() > InputRules.TEXT_LIMIT ? null : text;
    }

    private static Integer wholeNumberOf(JsonNode node, int min, int max) {
        if (!node.isNumber() || !node.canConvertToExactIntegral() || !node.canConvertToInt()) {
            return null;
        }
        int value = node.asInt();
        return value >= min && value <= max ? value : null;
    }

    private static LocalDate dateOf(JsonNode node) {
        if (!node.isString()) {
            return null;
        }
        try {
            return LocalDate.parse(node.asString().strip());
        } catch (DateTimeParseException unreadable) {
            return null;
        }
    }
}
