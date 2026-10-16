package com.studybuddy.matching.strategy;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Set;

/** Ranks purely by the weighted total across all criteria. */
@Component
public class BalancedStrategy implements MatchingStrategy {

    @Override
    public MatchingStrategyType type() {
        return MatchingStrategyType.BALANCED;
    }

    @Override
    public Comparator<MatchScore> ranking(Set<MatchingCriterion> enabled) {
        return Comparator.comparingDouble(MatchScore::total).reversed();
    }
}
