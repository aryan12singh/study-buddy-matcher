package com.studybuddy.matching.scoring;

/**
 * The criteria a match is scored on, each with the wording used in explanations and on the
 * administrator screen.
 */
public enum MatchingCriterion {
    COURSE("course"),
    AVAILABILITY("availability"),
    STUDY_MODE("meeting mode"),
    STUDY_GOAL("study goals"),
    GROUP_SIZE("group preference");

    private final String label;

    MatchingCriterion(String label) { this.label = label; }

    public String label() { return label; }
}
