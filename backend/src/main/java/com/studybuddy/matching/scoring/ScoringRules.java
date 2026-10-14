package com.studybuddy.matching.scoring;

/**
 * Tunable scoring values bound from {@code app.matching.scoring.*}, so no scorer
 * hides a number in its code.
 *
 * @param unknownScore what a criterion is worth when either student left it empty;
 *                     0 by default so missing data can never outrank real compatibility
 */
public record ScoringRules(double unknownScore) {

    public ScoringRules {
        requireFraction(unknownScore, "Unknown score");
    }

    private static void requireFraction(double value, String label) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(label + " must be between 0 and 1");
        }
    }
}
