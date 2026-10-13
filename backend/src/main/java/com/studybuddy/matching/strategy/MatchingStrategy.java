package com.studybuddy.matching.strategy;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;

import java.util.Comparator;
import java.util.Set;

/**
 * Orders scored matches. Each implementation uses its own administrator-configured
 * weighted total and applies its priority only while that criterion is enabled.
 */
public interface MatchingStrategy {

    MatchingStrategyType type();

    /** Orders scores best match first. */
    Comparator<MatchScore> ranking();

    /** Disabled priority criteria do not influence ranking. */
    default Comparator<MatchScore> ranking(Set<MatchingCriterion> enabled) {
        return ranking();
    }

}
