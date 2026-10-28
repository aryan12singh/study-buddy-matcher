package com.studybuddy.assistant;

/**
 * Carries out one kind of request using the app's existing services. Adding an ability to the
 * assistant means adding one implementation; {@link AssistantService} finds it by its type.
 */
public interface AssistantIntent {

    AssistantIntentType type();

    AssistantReplyDto reply(Long actorId, AssistantRequest request);
}
