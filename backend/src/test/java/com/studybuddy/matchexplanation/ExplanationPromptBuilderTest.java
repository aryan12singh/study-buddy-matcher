package com.studybuddy.matchexplanation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplanationPromptBuilderTest {
    private final ExplanationPromptBuilder builder = new ExplanationPromptBuilder(ExplanationFixtures.JSON);

    @Test
    void insertsTheEvidenceJsonAndNothingPersonal() {
        String prompt = builder.build(new ExplanationEvidenceFactory().from(ExplanationFixtures.evaluation()));

        assertTrue(prompt.contains("\"totalScore\":0.72"), prompt);
        assertTrue(prompt.contains("IS442"), prompt);
        assertFalse(prompt.contains("{{evidence}}"), prompt);
        assertFalse(prompt.contains("Jamie"), prompt);
    }

    @Test
    void asksForJsonAndForbidsProbabilityClaims() {
        String prompt = builder.build(new ExplanationEvidenceFactory().from(ExplanationFixtures.evaluation()));

        assertTrue(prompt.contains("{\"explanation\""), prompt);
        assertTrue(prompt.contains("probability"), prompt);
    }

    @Test
    void tellsTheModelThatAnUnsharedPreferenceIsNotAMismatch() {
        String prompt = builder.build(new ExplanationEvidenceFactory().from(ExplanationFixtures.evaluation()));

        assertEquals("v2", ExplanationPromptBuilder.PROMPT_VERSION);
        assertTrue(prompt.contains("\"known\": false"), prompt);
        assertTrue(prompt.contains("never as a weak or poor match"), prompt);
        assertTrue(prompt.contains("\"known\":true"), prompt);
    }
}
