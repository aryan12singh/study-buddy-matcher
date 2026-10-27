package com.studybuddy.assistant;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds {@code app.ai.assistant.chat.*}. */
@Configuration
@EnableConfigurationProperties(AssistantProperties.class)
public class AssistantConfiguration {
}
