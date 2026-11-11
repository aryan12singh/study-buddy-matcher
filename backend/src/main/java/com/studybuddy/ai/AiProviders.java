package com.studybuddy.ai;

/**
 * Builds each AI feature's client from its own Gemini client and the shared Claude client, in the
 * order {@code app.ai.primary-provider} sets. Every feature's Claude calls share one key, so they
 * also share one call allowance; each Gemini client keeps its own. With Claude switched off the
 * returned client behaves exactly like the Gemini client it wraps.
 */
public class AiProviders {
    private final AiProvider primary;
    private final ClaudeProperties claude;
    private final AiQuota claudeQuota;

    public AiProviders(AiProviderProperties providers, ClaudeProperties claude) {
        this.primary = providers.primaryProvider();
        this.claude = claude;
        this.claudeQuota = claude.newQuota();
    }

    /** For a short reply: one match explanation, or the reading of one Pixel message. */
    public AiClient forShortReplies(AiClient gemini, String purpose) {
        return inOrder(gemini, new ClaudeClient(claude, purpose, claude.maxTokens(), claudeQuota));
    }

    /** For a long reply: a whole quiz or study plan. */
    public AiClient forStudyTools(AiClient gemini, String purpose) {
        return inOrder(gemini, new ClaudeClient(claude, purpose, claude.studyToolsMaxTokens(), claudeQuota));
    }

    private AiClient inOrder(AiClient gemini, AiClient claudeClient) {
        return new FallbackAiClient(primary.inOrder(gemini, claudeClient));
    }
}
