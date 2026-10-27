package com.studybuddy.assistant;

import com.studybuddy.ai.AiClient;
import com.studybuddy.ai.AiUnavailableException;

/**
 * The AI that reads the assistant's messages, kept apart from the one that writes quizzes and study
 * plans: it has its own model and its own daily allowance (see {@link AssistantConfiguration}).
 * A class of its own so tests can supply a fake without replacing the quiz and study plan AI.
 */
public class AssistantAi {
    private final AiClient client;

    public AssistantAi(AiClient client) {
        this.client = client;
    }

    /** @throws AiUnavailableException when the AI is unavailable or over this allowance */
    public String generateJson(Long studentId, String prompt) {
        return client.generateJson(studentId, prompt);
    }
}
