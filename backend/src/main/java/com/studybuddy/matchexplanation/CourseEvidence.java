package com.studybuddy.matchexplanation;

/** The course the match was about. A course code is public catalogue data, not personal data. */
public record CourseEvidence(String code, String name) {
}
