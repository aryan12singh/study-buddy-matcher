package com.studybuddy.matching.config;

import com.studybuddy.matching.QualityThresholds;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.scoring.MatchingWeights;
import com.studybuddy.matching.scoring.ScoringRules;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import com.studybuddy.matching.strategy.PriorityTiers;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * The matching settings used until an admin saves their own, plus the quality
 * label cut-offs, enabled criteria, minimum score and result bounds, bound from {@code app.matching.*}. Each strategy has its own
 * default weights; a criterion left out of a strategy's weights counts as 0.
 * Invalid values stop the application starting.
 */
@Validated
@ConfigurationProperties("app.matching")
public record MatchingProperties(
        @NotNull MatchingStrategyType defaultStrategy,
        Map<MatchingStrategyType, Map<MatchingCriterion, Double>> defaultWeights,
        @NotNull QualityThresholds quality,
        @NotNull ScoringRules scoring,
        @NotNull PriorityTiers priorityTiers,
        Set<MatchingCriterion> defaultEnabledCriteria,
        double defaultMinScore, int defaultResultLimit, int maxResultLimit) {

    public MatchingProperties {
        if (defaultEnabledCriteria == null || defaultEnabledCriteria.isEmpty()) {
            throw new IllegalArgumentException("At least one default criterion must be enabled");
        }
        defaultEnabledCriteria = Set.copyOf(defaultEnabledCriteria);
        if (!Double.isFinite(defaultMinScore) || defaultMinScore < 0 || defaultMinScore > 1) {
            throw new IllegalArgumentException("Default minimum score must be between 0 and 1");
        }
        if (defaultResultLimit < 1 || maxResultLimit < defaultResultLimit) {
            throw new IllegalArgumentException("Result limits need 1 <= default <= maximum");
        }
        // Constructing each strategy's weights validates them, so a bad default fails at startup.
        for (MatchingStrategyType type : MatchingStrategyType.values()) {
            MatchingWeights weights = toWeights(type, defaultWeights);
            for (MatchingCriterion criterion : MatchingCriterion.values()) {
                if (!defaultEnabledCriteria.contains(criterion) && weights.weightOf(criterion) != 0.0) {
                    throw new IllegalArgumentException("A disabled default criterion must have zero weight");
                }
            }
        }
    }

    public MatchingWeights weightsFor(MatchingStrategyType type) {
        return toWeights(type, defaultWeights);
    }

    private static MatchingWeights toWeights(MatchingStrategyType type,
            Map<MatchingStrategyType, Map<MatchingCriterion, Double>> defaultWeights) {
        Map<MatchingCriterion, Double> configured = defaultWeights == null ? null : defaultWeights.get(type);
        if (configured == null) {
            throw new IllegalArgumentException("Default weights for " + type + " are required");
        }
        Map<MatchingCriterion, Double> values = new EnumMap<>(MatchingCriterion.class);
        for (MatchingCriterion criterion : MatchingCriterion.values()) {
            values.put(criterion, configured.getOrDefault(criterion, 0.0));
        }
        return new MatchingWeights(values);
    }
}
