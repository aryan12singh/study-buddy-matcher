package com.studybuddy.achievement;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AchievementProperties.class)
public class AchievementConfiguration {
}
