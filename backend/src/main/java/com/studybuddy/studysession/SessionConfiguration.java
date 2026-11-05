package com.studybuddy.studysession;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers {@link SessionSettings}, bound from {@code app.study-session}. */
@Configuration
@EnableConfigurationProperties(SessionSettings.class)
public class SessionConfiguration { }
