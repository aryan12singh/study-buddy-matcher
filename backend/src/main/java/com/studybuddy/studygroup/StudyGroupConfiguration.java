package com.studybuddy.studygroup;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StudyGroupProperties.class)
public class StudyGroupConfiguration {
}
