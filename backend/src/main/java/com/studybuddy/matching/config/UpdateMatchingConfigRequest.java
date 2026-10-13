package com.studybuddy.matching.config;

import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;

/** Complete settings update, guarded by the version last read by this administrator. */
public record UpdateMatchingConfigRequest(@NotNull MatchingStrategyType activeStrategy,
        @NotNull Map<MatchingStrategyType, Map<MatchingCriterion, Double>> weights,
        @NotNull Set<MatchingCriterion> enabledCriteria, @NotNull Double minScore,
        @NotNull Long expectedVersion) {
}
