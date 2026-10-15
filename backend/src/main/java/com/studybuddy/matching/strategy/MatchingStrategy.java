package com.studybuddy.matching.strategy;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;

import java.util.Comparator;
import java.util.Set;

/**
 * Orders scored matches, best first. Every strategy scores with its own
 * administrator-configured weights; the strategy only decides the order. A disabled
 * priority criterion does not influence ranking.
 */
public interface MatchingStrategy {

    MatchingStrategyType type();

    /** Orders scores best match first, given the criteria that are enabled. */
    Comparator<MatchScore> ranking(Set<MatchingCriterion> enabled);
}
