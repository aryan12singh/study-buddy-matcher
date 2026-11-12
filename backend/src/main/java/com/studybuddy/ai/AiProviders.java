package com.studybuddy.ai;

import com.anthropic.client.AnthropicClient;

import java.util.Optional;

/**
 * Builds each AI feature's client from its own Gemini client and the shared Claude connection, in
 * the order {@code app.ai.primary-provider} sets. Every feature's Claude calls share one key, so
 * they also share one SDK client and one call allowance; each Gemini client keeps its own. With Claude switched off the
 * returned client behaves exactly like the Gemini client it wraps.
 */
public class AiProviders {
    private final AiProvider primary;
    private final ClaudeProperties claude;
    private final AiQuota claudeQuota;
    private final Optional<AnthropicClient> claudeConnection;

    /** @param claudeConnection the one SDK client for Claude; empty when Claude is off or has no key */
    public AiProviders(AiProviderProperties providers, ClaudeProperties claude, Optional<AnthropicClient> claudeConnection) {
        this.primary = providers.primaryProvider();
        this.claude = claude;
        this.claudeQuota = claude.newQuota();
        this.claudeConnection = claudeConnection;
    }

    /** For a short reply: one match explanation, or the reading of one Pixel message. */
    public AiClient forShortReplies(AiClient gemini, String purpose) {
        return inOrder(gemini, new ClaudeClient(claude, purpose, claude.maxTokens(), claudeQuota, claudeConnection));
    }

    /** For a long reply: a whole quiz or study plan. */
    public AiClient forStudyTools(AiClient gemini, String purpose) {
        return inOrder(gemini, new ClaudeClient(claude, purpose, claude.studyToolsMaxTokens(), claudeQuota, claudeConnection));
    }

    private AiClient inOrder(AiClient gemini, AiClient claudeClient) {
        return new FallbackAiClient(primary.inOrder(gemini, claudeClient));
    }
}
