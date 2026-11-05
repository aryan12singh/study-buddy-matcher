package com.studybuddy.matchexplanation;

import com.studybuddy.ai.GeminiClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Binds {@code app.ai.explanation.*}, the same way the matching settings are bound, and builds E1's
 * explanation provider on a Gemini client of its own. It is not an {@code AiClient} bean, so the
 * quiz and study plan features can never pick up E1's key or allowance by accident.
 */
@Configuration
@EnableConfigurationProperties(AiExplanationProperties.class)
public class AiExplanationConfiguration {

    /** Wording a known score needs no reasoning step, so the model's thinking is turned off. */
    @Bean
    ExplanationProvider explanationProvider(AiExplanationProperties properties, JsonMapper json) {
        return new GeminiExplanationProvider(
                new GeminiClient(properties.geminiSettings("explanation", true), json, properties.newQuota()));
    }
}
