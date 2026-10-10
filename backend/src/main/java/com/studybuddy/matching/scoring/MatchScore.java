package com.studybuddy.matching.scoring;

import java.util.Map;

/**
 * The result of matching two students: the score on each criterion and the
 * combined total. Immutable; every score is between 0.0 and 1.0.
 */
public record MatchScore(Map<MatchingCriterion, Double> breakdown, double total) {

    public MatchScore {
        breakdown = Map.copyOf(breakdown);
        breakdown.forEach((criterion, score) -> requireInRange(score, criterion.name()));
        requireInRange(total, "total");
    }

    /** The score on one criterion, or 0.0 if the strategy did not score it. */
    public double scoreFor(MatchingCriterion criterion) {
        return breakdown.getOrDefault(criterion, 0.0);
    }

    private static void requireInRange(double score, String label) {
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException(label + " score must be between 0.0 and 1.0, was " + score);
        }
    }
}
