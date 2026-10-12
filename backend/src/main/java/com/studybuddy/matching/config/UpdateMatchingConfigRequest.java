package com.studybuddy.matching.config;

import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * What an admin supplies to change the matching settings: the active strategy
 * and weights for every strategy. A strategy's criterion it ranks by first may
 * be left out or set to 0. The annotations are for the controller's
 * {@code @Valid}; the service checks the rules again so it is safe to call
 * without one.
 */
public record UpdateMatchingConfigRequest(
        @NotNull MatchingStrategyType activeStrategy,
        @NotNull Map<MatchingStrategyType, Map<MatchingCriterion, Double>> weights) {
}
