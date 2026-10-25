package com.studybuddy.groupquiz;

import jakarta.validation.constraints.NotBlank;

/**
 * What a host starts a quiz on. The topics' length limit is configurable, so the service checks it.
 *
 * @param sessionId the group's session the quiz belongs to, or null for one started from the room
 */
public record StartQuizInput(@NotBlank(message = "Enter the topics to be quizzed on") String topics, Long sessionId) {
}
