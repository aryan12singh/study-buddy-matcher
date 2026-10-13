package com.studybuddy.matching.strategy;

public enum MatchingStrategyType {
    BALANCED("Balanced"),
    AVAILABILITY_FIRST("Availability first"),
    COURSE_FIRST("Course first");

    private final String label;

    MatchingStrategyType(String label) {
        this.label = label;
    }

    /** The name an admin sees, for use in messages. */
    public String label() {
        return label;
    }
}
