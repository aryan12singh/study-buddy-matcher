package com.studybuddy.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Demo data settings bound from {@code app.demo-seed}: whether to seed, and the passwords for the
 * seeded student and administrator accounts, which come from the environment.
 */
@ConfigurationProperties("app.demo-seed")
public record DemoSeedProperties(boolean enabled, String studentPassword, String adminPassword) {
}
