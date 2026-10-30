package com.studybuddy.studygroup;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers {@link StudyGroupProperties}, bound from {@code app.study-group}. */
@Configuration
@EnableConfigurationProperties(StudyGroupProperties.class)
public class StudyGroupConfiguration {
}
