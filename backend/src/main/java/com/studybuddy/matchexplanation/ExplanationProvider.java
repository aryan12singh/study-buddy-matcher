package com.studybuddy.matchexplanation;

import java.util.Optional;

/** Writes an explanation from a prompt. Empty means no usable answer; it never throws. */
public interface ExplanationProvider {
    Optional<String> explain(String prompt);
}
