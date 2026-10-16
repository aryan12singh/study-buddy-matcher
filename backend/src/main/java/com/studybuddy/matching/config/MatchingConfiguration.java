package com.studybuddy.matching.config;

import com.studybuddy.matching.QualityThresholds;
import com.studybuddy.matching.scoring.ScoringRules;
import com.studybuddy.matching.strategy.PriorityTiers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Binds {@code app.matching.*} and publishes the scoring values on their own, so the
 * scoring package depends on small value objects rather than on this config package.
 */
@Configuration
@EnableConfigurationProperties(MatchingProperties.class)
public class MatchingConfiguration {

    @Bean
    QualityThresholds qualityThresholds(MatchingProperties properties) {
        return properties.quality();
    }

    @Bean
    ScoringRules scoringRules(MatchingProperties properties) {
        return properties.scoring();
    }

    @Bean
    PriorityTiers priorityTiers(MatchingProperties properties) {
        return properties.priorityTiers();
    }
}
