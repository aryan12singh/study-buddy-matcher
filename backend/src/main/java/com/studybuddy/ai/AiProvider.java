package com.studybuddy.ai;

import java.util.List;

/**
 * The AI providers the app can call. The one named by {@code app.ai.primary-provider} is asked
 * first and the other only when it cannot answer. Each constant knows its own order, so no caller
 * needs a {@code switch} on the provider.
 */
public enum AiProvider {
    GEMINI {
        @Override
        List<AiClient> inOrder(AiClient gemini, AiClient claude) {
            return List.of(gemini, claude);
        }
    },
    CLAUDE {
        @Override
        List<AiClient> inOrder(AiClient gemini, AiClient claude) {
            return List.of(claude, gemini);
        }
    };

    /** The two clients with this provider's first. */
    abstract List<AiClient> inOrder(AiClient gemini, AiClient claude);
}
