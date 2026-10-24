package com.studybuddy.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds {@code app.ai.assistant.*}, the same way the E1 explanation settings are bound. */
@Configuration
@EnableConfigurationProperties(AiAssistantProperties.class)
public class AiAssistantConfiguration {
}
