package com.studybuddy.assistant;

import java.util.Map;

/**
 * The next step a reply offers, such as "See all on Find buddies".
 *
 * @param link the app path to open, for {@link AssistantActionKind#OPEN_PAGE}; null otherwise
 * @param prefill details the student gave, for the opened page or Quiz Buddy to fill in
 */
public record AssistantActionDto(AssistantActionKind kind, String label, String link, Map<String, String> prefill) {
    public AssistantActionDto {
        prefill = prefill == null ? Map.of() : Map.copyOf(prefill);
    }

    public static AssistantActionDto openPage(String label, String link) {
        return new AssistantActionDto(AssistantActionKind.OPEN_PAGE, label, link, Map.of());
    }
}
