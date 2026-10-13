package com.studybuddy.matching.scoring;

import java.util.Map;
import com.studybuddy.matching.MatchQuality;

/**
 * The result of matching two students: the score on each criterion and the
 * combined total, unique shared hours, quality and deterministic explanation.
 * Immutable; every compatibility score is finite and between 0.0 and 1.0.
 */
public record MatchScore(Map<MatchingCriterion, Double> breakdown, double total,
        double sharedHoursPerWeek, String explanation, MatchQuality quality) {

    public MatchScore {
        breakdown = Map.copyOf(breakdown);
        breakdown.forEach((criterion, score) -> requireInRange(score, criterion.name()));
        requireInRange(total, "total");
        if (!Double.isFinite(sharedHoursPerWeek) || sharedHoursPerWeek < 0 || explanation == null || quality == null) {
            throw new IllegalArgumentException("Shared hours, explanation and quality are required");
        }
    }

    public MatchScore(Map<MatchingCriterion, Double> breakdown, double total) {
        this(breakdown, total, 0.0, "", MatchQuality.FAIR);
    }

    /** The score on one criterion, or 0.0 if the strategy did not score it. */
    public double scoreFor(MatchingCriterion criterion) {
        return breakdown.getOrDefault(criterion, 0.0);
    }

    private static void requireInRange(double score, String label) {
        if (!Double.isFinite(score) || score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException(label + " score must be between 0.0 and 1.0, was " + score);
        }
    }
}
