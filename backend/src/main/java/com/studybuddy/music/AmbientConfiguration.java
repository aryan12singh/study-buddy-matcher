package com.studybuddy.music;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.random.RandomGenerator;

/**
 * Registers {@link AmbientProperties} ({@code app.ambient}) and supplies the random source used to
 * pick a sound.
 */
@Configuration
@EnableConfigurationProperties(AmbientProperties.class)
public class AmbientConfiguration {
    /** A bean rather than a static call so tests can make the pick predictable. */
    @Bean
    public RandomGenerator ambientRandom() {
        return RandomGenerator.getDefault();
    }
}
