package com.studybuddy.matching.config;

import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.strategy.MatchingStrategyType;

import java.util.Map;

/**
 * The matching settings currently in force, as shown to an admin: the active
 * strategy and each strategy's weights. A strategy's weights list only the
 * criteria it weights, so the criterion it ranks by first is absent.
 */
public record MatchingConfigDto(
        MatchingStrategyType activeStrategy,
        Map<MatchingStrategyType, Map<MatchingCriterion, Double>> weights) {
}
