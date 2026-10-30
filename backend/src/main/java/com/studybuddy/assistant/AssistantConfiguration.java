package com.studybuddy.assistant;

import com.studybuddy.ai.AiAssistantProperties;
import com.studybuddy.ai.GeminiClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** Binds {@code app.ai.assistant.chat.*} and builds the assistant's own AI client. */
@Configuration
@EnableConfigurationProperties(AssistantProperties.class)
public class AssistantConfiguration {

    /**
     * The shared assistant settings with the chat's own model, size, timeout and daily limits, and a
     * separate quota counter. Lite models have no thinking step and reject the setting that turns it
     * off, so it is left out.
     */
    @Bean
    AssistantAi assistantAi(AiAssistantProperties shared, AssistantProperties chat, JsonMapper json) {
        AiAssistantProperties forChat = new AiAssistantProperties(shared.enabled(), shared.baseUrl(), chat.model(),
                shared.apiKey(), chat.timeout(), chat.maxOutputTokens(), chat.dailyCallLimit(),
                chat.dailyCallLimitPerStudent(), shared.quotaZone(), shared.rateLimitCooldown());
        return new AssistantAi(new GeminiClient(forChat.geminiSettings("assistant chat", false), json, forChat.newQuota()));
    }
}
