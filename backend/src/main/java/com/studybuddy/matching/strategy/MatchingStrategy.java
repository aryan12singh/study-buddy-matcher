package com.studybuddy.matching.strategy;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

/**
 * Decides the order matches are shown in and which criteria count towards the
 * total score. Each strategy has its own admin-configured weights. Each strategy
 * is its own implementation, so choosing one never needs a switch on its type.
 */
public interface MatchingStrategy {

    MatchingStrategyType type();

    /** Orders scores best match first. */
    Comparator<MatchScore> ranking();

    /**
     * The criteria this strategy weights in the total score. A strategy that
     * already ranks by a criterion leaves it out, so it is not counted twice.
     */
    default Set<MatchingCriterion> weightedCriteria() {
        return EnumSet.allOf(MatchingCriterion.class);
    }
}
