package com.studybuddy.achievement;

/** Medal tiers in ascending order; {@link #points()} feeds the level summary. */
public enum BadgeTier {
    BRONZE(1), SILVER(2), GOLD(3);

    private final int points;

    BadgeTier(int points) {
        this.points = points;
    }

    public int points() {
        return points;
    }

    /** True when this tier is higher than {@code other}; every tier is above "not earned" (null). */
    public boolean isAbove(BadgeTier other) {
        return other == null || compareTo(other) > 0;
    }

    /** The higher of two tiers, either of which may be null for "not earned". */
    public static BadgeTier higher(BadgeTier first, BadgeTier second) {
        if (first == null) return second;
        return first.isAbove(second) ? first : second;
    }
}
