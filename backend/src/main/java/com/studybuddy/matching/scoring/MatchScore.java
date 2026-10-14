package com.studybuddy.matching.scoring;

import com.studybuddy.matching.MatchQuality;

import java.util.Map;
import java.util.Set;

/**
 * The result of matching two students: the score on each criterion and the
 * combined total, shared hours and quality label.
 * Immutable; every compatibility score is finite and between 0.0 and 1.0. Criteria
 * in {@code unknownCriteria} had no data on one side, so their breakdown value is
 * the configured unknown score rather than a measured one.
 */
public record MatchScore(Map<MatchingCriterion, Double> breakdown, double total,
        double sharedHoursPerWeek, MatchQuality quality, Set<MatchingCriterion> unknownCriteria) {

    public MatchScore {
        breakdown = Map.copyOf(breakdown);
        breakdown.forEach((criterion, score) -> requireInRange(score, criterion.name()));
        requireInRange(total, "total");
        if (!Double.isFinite(sharedHoursPerWeek) || sharedHoursPerWeek < 0 || quality == null) {
            throw new IllegalArgumentException("Shared hours and quality are required");
        }
        unknownCriteria = unknownCriteria == null ? Set.of() : Set.copyOf(unknownCriteria);
    }

    /** The score on one criterion, or 0.0 if the strategy did not score it. */
    public double scoreFor(MatchingCriterion criterion) {
        return breakdown.getOrDefault(criterion, 0.0);
    }

    /** False when either student left this preference empty. */
    public boolean isKnown(MatchingCriterion criterion) {
        return !unknownCriteria.contains(criterion);
    }

    private static void requireInRange(double score, String label) {
        if (!Double.isFinite(score) || score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException(label + " score must be between 0.0 and 1.0, was " + score);
        }
    }
}
