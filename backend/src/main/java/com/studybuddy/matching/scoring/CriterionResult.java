package com.studybuddy.matching.scoring;

/**
 * One criterion's compatibility between two students. Either a known score from
 * 0.0 to 1.0, or unknown because one of them has not filled in that preference.
 * Keeping "unknown" separate from any number lets the weighted total decide what
 * missing data is worth, and lets the result card say "not shared" honestly.
 */
public record CriterionResult(double score, boolean known) {

    private static final CriterionResult UNKNOWN = new CriterionResult(0.0, false);

    public CriterionResult {
        if (!Double.isFinite(score) || score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("A criterion score must be between 0.0 and 1.0, was " + score);
        }
    }

    public static CriterionResult of(double score) {
        return new CriterionResult(score, true);
    }

    public static CriterionResult unknown() {
        return UNKNOWN;
    }
}
