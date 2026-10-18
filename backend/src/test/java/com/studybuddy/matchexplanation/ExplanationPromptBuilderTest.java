package com.studybuddy.matchexplanation;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

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
        assertTrue(prompt.contains("scoringCourse"), prompt);
        assertTrue(prompt.contains("sharedCourse"), prompt);
        assertTrue(prompt.contains("searchedGoal"), prompt);
    }

    @Test
    void tellsTheModelThatAnUnsharedPreferenceIsNotAMismatch() {
        String prompt = builder.build(new ExplanationEvidenceFactory().from(ExplanationFixtures.evaluation()));

        assertEquals("v5", ExplanationPromptBuilder.PROMPT_VERSION);
        assertTrue(prompt.contains("\"known\": false"), prompt);
        assertTrue(prompt.contains("never as a weak or poor match"), prompt);
        assertTrue(prompt.contains("\"known\":true"), prompt);
    }

    /** A code-style word such as EXAM_PREPARATION or STRONG; course codes like IS442 contain digits and do not match. */
    private static final Pattern CODE_STYLE = Pattern.compile("\\b[A-Z][A-Z_]{2,}\\b");

    @Test
    void sendsEveryValueToTheModelInPlainWords() {
        String prompt = builder.build(new ExplanationEvidenceFactory().from(ExplanationFixtures.evaluation()));
        String evidence = prompt.substring(prompt.indexOf("Evidence:"));

        assertFalse(CODE_STYLE.matcher(evidence).find(), evidence);
        assertTrue(evidence.contains("\"criterion\":\"meeting mode\""), evidence);
        assertTrue(evidence.contains("\"sharedGoals\":[\"exam preparation\"]"), evidence);
        assertTrue(evidence.contains("\"searcherGroupSize\":\"one to one\""), evidence);
        assertTrue(evidence.contains("\"quality\":\"good\""), evidence);
        assertTrue(evidence.contains("IS442"), evidence);
    }

    @Test
    void tellsTheModelToWriteValuesInPlainWordsWithNormalCapitalisation() {
        String prompt = builder.build(new ExplanationEvidenceFactory().from(ExplanationFixtures.evaluation()));

        assertTrue(prompt.contains("plain words"), prompt);
        assertTrue(prompt.contains("normal sentence capitalisation"), prompt);
        assertFalse(prompt.contains("lowercase"), prompt);
    }

    @Test
    void sendsWeightsButNotTheAdminStrategyName() {
        String prompt = builder.build(new ExplanationEvidenceFactory().from(ExplanationFixtures.evaluation()));
        String evidence = prompt.substring(prompt.indexOf("Evidence:"));

        assertTrue(evidence.contains("\"weight\":0.3"), evidence);
        assertFalse(evidence.contains("strategy"), evidence);
        assertFalse(evidence.contains("Balanced"), evidence);
    }

    @Test
    void givesTheModelAnExampleReplyAndGroupedRules() {
        String prompt = builder.build(new ExplanationEvidenceFactory().from(ExplanationFixtures.evaluation()));

        assertTrue(prompt.contains("Example reply"), prompt);
        assertTrue(prompt.contains("You must"), prompt);
        assertTrue(prompt.contains("You must not"), prompt);
    }
}
