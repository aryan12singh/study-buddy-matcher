package com.studybuddy.matching.config;

import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.EnumSet;

/** Effective settings; version changes on every successful save or explicit restore. */
public record MatchingConfigDto(MatchingStrategyType activeStrategy,
        Map<MatchingStrategyType, Map<MatchingCriterion, Double>> weights,
        Set<MatchingCriterion> enabledCriteria, double minScore, long version, boolean usingDefaults) {
    public MatchingConfigDto {
        enabledCriteria = Collections.unmodifiableSet(EnumSet.copyOf(enabledCriteria));
    }
}
