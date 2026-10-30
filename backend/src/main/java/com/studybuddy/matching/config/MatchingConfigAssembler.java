package com.studybuddy.matching.config;

import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.scoring.MatchingWeights;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Builds the {@link MatchingConfigDto} the administrator screen edits, copying the weights per
 * strategy so the DTO shares no mutable state with the domain objects.
 */
@Component
public class MatchingConfigAssembler {
    public MatchingConfigDto toDto(MatchingStrategyType strategy, Map<MatchingStrategyType, MatchingWeights> weights,
            Set<MatchingCriterion> enabled, double minScore, long version, boolean usingDefaults) {
        Map<MatchingStrategyType, Map<MatchingCriterion, Double>> byStrategy = new EnumMap<>(MatchingStrategyType.class);
        weights.forEach((type, values) -> byStrategy.put(type, new EnumMap<>(values.values())));
        return new MatchingConfigDto(strategy, byStrategy, Set.copyOf(enabled), minScore, version, usingDefaults);
    }
}
