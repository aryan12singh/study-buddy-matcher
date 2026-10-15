package com.studybuddy.matchexplanation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableConfigurationProperties(AiExplanationProperties.class)
public class AiExplanationConfiguration {

    /** The day boundary for the call quota follows the students' time zone, not the server's. */
    @Bean
    public Clock explanationQuotaClock(AiExplanationProperties properties) {
        return Clock.system(ZoneId.of(properties.quotaZone()));
    }

    @Bean
    public ExplanationOutputValidator explanationOutputValidator(AiExplanationProperties properties, JsonMapper json) {
        return new ExplanationOutputValidator(properties.maxCharacters(), json);
    }
}
