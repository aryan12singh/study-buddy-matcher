package com.studybuddy.ai;

import java.util.List;

/**
 * Asks the AI providers in order and returns the first answer. The next provider is asked only when
 * the one before cannot answer: switched off, over its allowance, rate limited, timed out, failing
 * or returning an unusable reply. When none can, the last {@link AiUnavailableException} reaches the
 * feature, which then takes its usual non-AI path. The features see one {@link AiClient} and need
 * not know which provider replied.
 */
public class FallbackAiClient implements AiClient {
    private final List<AiClient> providers;

    /** @param providers the clients to ask, first choice first; at least one */
    public FallbackAiClient(List<AiClient> providers) {
        if (providers.isEmpty()) {
            throw new IllegalArgumentException("At least one AI provider is needed");
        }
        this.providers = List.copyOf(providers);
    }

    @Override
    public String generateJson(Long studentId, String prompt, List<AiAttachment> attachments) {
        AiUnavailableException last = null;
        for (AiClient provider : providers) {
            try {
                return provider.generateJson(studentId, prompt, attachments);
            } catch (AiUnavailableException unavailable) {
                last = unavailable;
            }
        }
        throw last;
    }
}
