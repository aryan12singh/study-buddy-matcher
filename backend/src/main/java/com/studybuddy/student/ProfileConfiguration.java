package com.studybuddy.student;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ProfileProperties.class)
public class ProfileConfiguration {
}
