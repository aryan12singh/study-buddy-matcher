package com.studybuddy.forest;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ForestProperties.class)
public class ForestConfiguration {
    @Bean
    public RandomGenerator forestRandom() {
        return new SecureRandom();
    }
}
