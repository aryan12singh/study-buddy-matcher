package com.studybuddy.matching.config;

import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.scoring.MatchingWeights;
import com.studybuddy.matching.strategy.MatchingStrategy;
import java.util.Set;
import java.util.Collections;
import java.util.EnumSet;

/** One coherent read, shared by scoring, ranking, filtering and result metadata. */
public record MatchingConfigSnapshot(MatchingStrategy strategy, MatchingWeights weights,
        Set<MatchingCriterion> enabledCriteria, double minScore, long version) {
    public MatchingConfigSnapshot { enabledCriteria = Collections.unmodifiableSet(EnumSet.copyOf(enabledCriteria)); }
}
