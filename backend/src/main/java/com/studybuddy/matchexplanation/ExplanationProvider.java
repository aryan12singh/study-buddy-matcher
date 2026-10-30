package com.studybuddy.matchexplanation;

import java.util.Optional;

/**
 * Writes an explanation from a prompt, within the asking student's share of the call allowance.
 * Empty means no usable answer (switched off, over quota, rate limited or failing); it never throws.
 */
public interface ExplanationProvider {
    Optional<String> explain(Long studentId, String prompt);
}
