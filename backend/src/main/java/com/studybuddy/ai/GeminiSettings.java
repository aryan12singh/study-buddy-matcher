package com.studybuddy.ai;

import java.time.Duration;

/**
 * How one {@link GeminiClient} reaches Gemini: its switch, key, model, timeout and reply size. Each
 * feature builds its own from its configuration, so the study tools, the assistant chat and E1
 * explanations can use different keys and models with the same client code.
 *
 * @param purpose names the feature in log lines, such as "explanation"; never sent to Gemini
 * @param turnOffThinking whether to ask the model to skip its thinking step; models without one,
 *        such as the lite models, reject the setting, so it must then be left out
 */
public record GeminiSettings(
        String purpose,
        boolean enabled,
        String baseUrl,
        String model,
        String apiKey,
        Duration timeout,
        int maxOutputTokens,
        boolean turnOffThinking) {

    /** True only when the feature is switched on and a key is configured. */
    public boolean ready() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /** Never print the key, even in a startup error. */
    @Override
    public String toString() {
        return "GeminiSettings[purpose=" + purpose + ", enabled=" + enabled + ", model=" + model + ", apiKey="
                + (apiKey == null ? "absent" : "set") + "]";
    }
}
