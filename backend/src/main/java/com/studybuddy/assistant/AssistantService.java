package com.studybuddy.assistant;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.InvalidInputException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Answers one message: the AI says what it asks for, then the matching {@link AssistantIntent}
 * answers it from the app's own services. Intents are looked up by type rather than chosen in a
 * {@code switch}, so a new ability is one new class. Each message is handled on its own, without
 * memory of earlier ones. Deliberately not transactional, because the AI call can take seconds.
 */
@Service
public class AssistantService {
    private final Map<AssistantIntentType, AssistantIntent> intents = new EnumMap<>(AssistantIntentType.class);
    private final IntentParser parser;
    private final AccountAccess accounts;
    private final AssistantProperties properties;

    public AssistantService(List<AssistantIntent> intents, IntentParser parser, AccountAccess accounts,
            AssistantProperties properties) {
        intents.forEach(intent -> this.intents.put(intent.type(), intent));
        for (AssistantIntentType type : AssistantIntentType.values()) {
            if (!this.intents.containsKey(type)) {
                throw new IllegalStateException("No assistant intent handles " + type);
            }
        }
        this.parser = parser;
        this.accounts = accounts;
        this.properties = properties;
    }

    /** Checks the message before the AI call, so a bad one never spends the student's allowance. */
    public AssistantReplyDto reply(Long actorId, AssistantMessageInput input) {
        accounts.requireStudent(actorId);
        String message = input.message().strip();
        if (message.length() > properties.maxMessageCharacters()) {
            throw new InvalidInputException("message", "Keep messages under " + properties.maxMessageCharacters() + " characters");
        }
        AssistantRequest request = parser.parse(actorId, message);
        return intents.get(request.intent()).reply(actorId, request);
    }
}
