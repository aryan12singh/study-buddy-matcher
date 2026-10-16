package com.studybuddy.matchexplanation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(AiExplanationProperties.class)
public class AiExplanationConfiguration {

    @Bean
    public ExplanationOutputValidator explanationOutputValidator(AiExplanationProperties properties, JsonMapper json) {
        return new ExplanationOutputValidator(properties.maxCharacters(), json);
    }
}
