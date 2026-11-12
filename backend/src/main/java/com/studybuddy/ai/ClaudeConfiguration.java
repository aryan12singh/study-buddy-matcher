package com.studybuddy.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the one connection to Claude that every AI feature shares. The SDK client owns a
 * connection pool and worker threads, so the application keeps a single one for the single key
 * and server, and Spring closes it on shutdown. What differs per feature (reply size, the name in
 * log lines) is set on each request by {@link ClaudeClient}, not here.
 */
@Configuration
public class ClaudeConfiguration {
    /** A failed call moves on to the next provider or the non-AI path rather than being repeated. */
    private static final int NO_RETRIES = 0;

    /**
     * Exists only when Claude is switched on with a key, so the application starts the same way
     * without one. {@code app.ai.claude.timeout} applies to every feature's Claude calls.
     */
    @Bean(destroyMethod = "close")
    @Conditional(ClaudeReadyCondition.class)
    AnthropicClient anthropicClient(ClaudeProperties properties) {
        return AnthropicOkHttpClient.builder().apiKey(properties.apiKey()).baseUrl(properties.baseUrl())
                .timeout(properties.timeout()).maxRetries(NO_RETRIES).build();
    }
}
