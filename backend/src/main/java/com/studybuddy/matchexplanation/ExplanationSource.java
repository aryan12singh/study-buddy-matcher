package com.studybuddy.matchexplanation;

/** Where an explanation's text came from, so the screen can label it honestly. */
public enum ExplanationSource {
    /** Written by the AI provider from anonymous evidence. */
    AI,
    /** The matching engine's own deterministic explanation. */
    STANDARD
}
