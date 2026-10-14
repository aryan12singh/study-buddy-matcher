package com.studybuddy.studysession;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SessionSettings.class)
public class SessionConfiguration { }
