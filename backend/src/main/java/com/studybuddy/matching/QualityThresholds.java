package com.studybuddy.matching;

/**
 * The total scores at which a match counts as strong or good, bound from
 * {@code app.matching.quality.*}. Anything below good is fair.
 */
public record QualityThresholds(double strong, double good) {

    public QualityThresholds {
        if (good < 0.0 || strong > 1.0 || good > strong) {
            throw new IllegalArgumentException("Quality thresholds need 0 <= good <= strong <= 1");
        }
    }

    public MatchQuality classify(double total) {
        if (total >= strong) {
            return MatchQuality.STRONG;
        }
        return total >= good ? MatchQuality.GOOD : MatchQuality.FAIR;
    }
}
