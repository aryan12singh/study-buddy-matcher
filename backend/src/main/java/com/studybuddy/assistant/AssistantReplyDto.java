package com.studybuddy.assistant;

import com.studybuddy.matching.MatchResultDto;
import com.studybuddy.matching.SharedSlotDto;

import java.util.List;

/**
 * The assistant's answer. {@code text} is written by the app from templates, never by the AI; the
 * matches and slots come from the matching engine and session availability, so they are real.
 * Matches are the same privacy-safe cards as Find buddies, without contact numbers.
 *
 * @param filters the search filters the answer used, so the student can see what was understood
 * @param todos things waiting for the student, each linking to where they can act on it
 * @param action the next step offered, or null
 */
public record AssistantReplyDto(
        AssistantIntentType intent,
        String text,
        List<String> filters,
        List<MatchResultDto> matches,
        List<SharedSlotDto> slots,
        List<AssistantTodoDto> todos,
        AssistantActionDto action) {

    public AssistantReplyDto {
        filters = filters == null ? List.of() : List.copyOf(filters);
        matches = matches == null ? List.of() : List.copyOf(matches);
        slots = slots == null ? List.of() : List.copyOf(slots);
        todos = todos == null ? List.of() : List.copyOf(todos);
    }

    /** A reply that is only words, with an optional next step. */
    public static AssistantReplyDto text(AssistantIntentType intent, String text, AssistantActionDto action) {
        return new AssistantReplyDto(intent, text, List.of(), List.of(), List.of(), List.of(), action);
    }
}
