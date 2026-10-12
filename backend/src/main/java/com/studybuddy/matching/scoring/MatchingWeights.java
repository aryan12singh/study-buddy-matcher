package com.studybuddy.matching.scoring;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

/**
 * How much each criterion counts towards a match. Every criterion has a weight
 * between 0 and 1, and the weights add up to 1, so each weight is that
 * criterion's share of the total score.
 */
public record MatchingWeights(Map<MatchingCriterion, Double> values) {

    /** Allows for rounding in typed decimals such as 0.1 + 0.2. */
    public static final double SUM_TOLERANCE = 0.001;

    /** Rescaled weights are rounded to hundredths, a whole percentage each. */
    private static final int HUNDREDTHS = 100;

    public MatchingWeights {
        requireEveryWeight(values);
        values = Map.copyOf(values);
        double sum = sum(values);
        if (Math.abs(sum - 1.0) > SUM_TOLERANCE) {
            throw new IllegalArgumentException("The weights must add up to 1, but add up to " + Math.round(sum * 1000) / 1000.0);
        }
    }

    /**
     * Rescales weights saved before they had to add up to 1, keeping each
     * criterion's share of the total as close as two decimal places allow.
     */
    public static MatchingWeights rescaledToOne(Map<MatchingCriterion, Double> values) {
        requireEveryWeight(values);
        double sum = sum(values);
        if (sum <= 0.0) {
            throw new IllegalArgumentException("At least one weight must be above zero");
        }
        // Work in whole hundredths: round each share down, then hand the hundredths left over
        // to the shares that lost the most in rounding, so the total is exactly 1.
        Map<MatchingCriterion, Double> exact = new EnumMap<>(MatchingCriterion.class);
        Map<MatchingCriterion, Integer> hundredths = new EnumMap<>(MatchingCriterion.class);
        values.forEach((criterion, weight) -> {
            double share = weight / sum * HUNDREDTHS;
            exact.put(criterion, share);
            hundredths.put(criterion, (int) Math.floor(share));
        });
        int leftOver = HUNDREDTHS - hundredths.values().stream().mapToInt(Integer::intValue).sum();
        exact.keySet().stream()
                .sorted(Comparator.comparingDouble((MatchingCriterion criterion) -> exact.get(criterion) - hundredths.get(criterion))
                        .reversed())
                .limit(leftOver)
                .forEach(criterion -> hundredths.merge(criterion, 1, Integer::sum));
        Map<MatchingCriterion, Double> rescaled = new EnumMap<>(MatchingCriterion.class);
        hundredths.forEach((criterion, share) -> rescaled.put(criterion, share / (double) HUNDREDTHS));
        return new MatchingWeights(rescaled);
    }

    public double weightOf(MatchingCriterion criterion) {
        return values.get(criterion);
    }

    private static void requireEveryWeight(Map<MatchingCriterion, Double> values) {
        if (values == null) {
            throw new IllegalArgumentException("Weights are required");
        }
        for (MatchingCriterion criterion : MatchingCriterion.values()) {
            Double weight = values.get(criterion);
            if (weight == null) {
                throw new IllegalArgumentException("A weight for " + criterion + " is required");
            }
            if (!Double.isFinite(weight) || weight < 0.0) {
                throw new IllegalArgumentException("The weight for " + criterion + " must be zero or more");
            }
        }
    }

    private static double sum(Map<MatchingCriterion, Double> values) {
        return values.values().stream().mapToDouble(Double::doubleValue).sum();
    }
}
