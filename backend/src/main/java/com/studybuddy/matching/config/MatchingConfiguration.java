package com.studybuddy.matching.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MatchingProperties.class)
public class MatchingConfiguration {
}
