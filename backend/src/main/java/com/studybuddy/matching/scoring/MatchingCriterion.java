package com.studybuddy.matching.scoring;

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
