package com.studybuddy.studyplan;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds {@code app.ai.assistant.study-plan.*}. */
@Configuration
@EnableConfigurationProperties(StudyPlanProperties.class)
public class StudyPlanConfiguration {
}
