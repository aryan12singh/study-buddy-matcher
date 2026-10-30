package com.studybuddy.matchexplanation;

import com.studybuddy.ai.AiClient;
import com.studybuddy.ai.AiUnavailableException;

import java.util.Optional;

/**
 * Adapts E1's own Gemini client (its own key, model, timeouts and allowance; see
 * {@link AiExplanationConfiguration}) to {@link ExplanationProvider}: any failure the client
 * reports becomes an empty answer, so the standard explanation is shown instead.
 */
public class GeminiExplanationProvider implements ExplanationProvider {
    private final AiClient client;

    public GeminiExplanationProvider(AiClient client) {
        this.client = client;
    }

    @Override
    public Optional<String> explain(Long studentId, String prompt) {
        try {
            return Optional.of(client.generateJson(studentId, prompt));
        } catch (AiUnavailableException unavailable) {
            // Already logged by the client without the prompt or key.
            return Optional.empty();
        }
    }
}
