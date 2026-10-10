package com.studybuddy.matching.config;

import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.scoring.MatchingWeights;
import com.studybuddy.matching.strategy.MatchingStrategy;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class MatchingConfigAssembler {

    public MatchingConfigDto toDto(MatchingStrategyType activeStrategy, Map<MatchingStrategyType, MatchingWeights> weights,
            Map<MatchingStrategyType, MatchingStrategy> strategies) {
        // EnumMaps keep strategies and criteria in declaration order in the JSON response.
        Map<MatchingStrategyType, Map<MatchingCriterion, Double>> byStrategy = new EnumMap<>(MatchingStrategyType.class);
        weights.forEach((type, strategyWeights) -> {
            Map<MatchingCriterion, Double> weighted = new EnumMap<>(MatchingCriterion.class);
            strategies.get(type).weightedCriteria()
                    .forEach(criterion -> weighted.put(criterion, strategyWeights.weightOf(criterion)));
            byStrategy.put(type, weighted);
        });
        return new MatchingConfigDto(activeStrategy, byStrategy);
    }
}
