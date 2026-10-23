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
}
