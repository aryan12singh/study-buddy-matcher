package com.studybuddy.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Binds {@code app.ai.assistant.*}, the same way the E1 explanation settings are bound, and builds
 * the AI client the quiz and study plan features share, with its own call allowance.
 */
@Configuration
@EnableConfigurationProperties(AiAssistantProperties.class)
public class AiAssistantConfiguration {

    /** Quizzes and roadmaps run on a full model, so its thinking step is turned off to save tokens. */
    @Bean
    AiClient studyToolsAi(AiAssistantProperties properties, JsonMapper json) {
        return new GeminiClient(properties.geminiSettings("assistant", true), json, properties.newQuota());
    }
}
