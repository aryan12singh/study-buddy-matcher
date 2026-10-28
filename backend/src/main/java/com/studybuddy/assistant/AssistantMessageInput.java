package com.studybuddy.assistant;

import jakarta.validation.constraints.NotBlank;

/** One message to the assistant. Its configurable length limit is checked by the service. */
public record AssistantMessageInput(@NotBlank(message = "Type a message for the assistant") String message) {
}
