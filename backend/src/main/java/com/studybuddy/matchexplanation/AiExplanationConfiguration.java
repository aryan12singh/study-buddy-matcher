package com.studybuddy.matchexplanation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds {@code app.ai.explanation.*}, the same way the matching settings are bound. */
@Configuration
@EnableConfigurationProperties(AiExplanationProperties.class)
public class AiExplanationConfiguration {
}
