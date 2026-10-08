package com.studybuddy.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.demo-seed")
public record DemoSeedProperties(boolean enabled, String studentPassword, String adminPassword) {
}
