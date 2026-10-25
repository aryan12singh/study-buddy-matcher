package com.studybuddy.quiz;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds {@code app.ai.assistant.quiz.*}. */
@Configuration
@EnableConfigurationProperties(QuizProperties.class)
public class QuizConfiguration {
}
