package com.studybuddy.achievement;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers {@link AchievementProperties}, bound from the {@code app.achievements} keys. */
@Configuration
@EnableConfigurationProperties(AchievementProperties.class)
public class AchievementConfiguration {
}
