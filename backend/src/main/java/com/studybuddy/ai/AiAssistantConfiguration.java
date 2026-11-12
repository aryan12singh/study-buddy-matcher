package com.studybuddy.ai;

import com.anthropic.client.AnthropicClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

/**
 * Binds {@code app.ai.assistant.*}, the same way the E1 explanation settings are bound, and builds
 * the AI client the quiz and study plan features share, with its own call allowance. Also binds
 * {@code app.ai.claude.*} and {@code app.ai.primary-provider}, which set the second provider and the
 * order the two are asked in.
 */
@Configuration
@EnableConfigurationProperties({AiAssistantProperties.class, ClaudeProperties.class, AiProviderProperties.class})
public class AiAssistantConfiguration {

    /** Quizzes and roadmaps run on a full model, so its thinking step is turned off to save tokens. */
    @Bean
    AiClient studyToolsAi(AiAssistantProperties properties, JsonMapper json, AiProviders providers) {
        return providers.forStudyTools(
                new GeminiClient(properties.geminiSettings("assistant", true), json, properties.newQuota()), "assistant");
    }

    /**
     * Orders each feature's Gemini client and the shared Claude connection by the configured primary
     * provider. The connection is absent when Claude is off or has no key.
     */
    @Bean
    AiProviders aiProviders(AiProviderProperties providers, ClaudeProperties claude, Optional<AnthropicClient> claudeConnection) {
        return new AiProviders(providers, claude, claudeConnection);
    }
}
