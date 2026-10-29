package com.studybuddy.forest;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ForestProperties.class)
public class ForestConfiguration {
}
