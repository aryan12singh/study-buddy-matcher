package com.studybuddy.matchexplanation;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplanationOutputValidatorTest {
    private final ExplanationEvidence evidence = new ExplanationEvidenceFactory().from(ExplanationFixtures.evaluation());
    private final ExplanationOutputValidator validator = new ExplanationOutputValidator(120, ExplanationFixtures.JSON);

    @Test
    void acceptsAShortGroundedExplanationAndTrimsIt() {
        assertEquals(Optional.of("You both take IS442 and share 3.5 free hours a week."),
                validate("{\"explanation\": \"  You both take IS442 and share 3.5 free hours a week. \"}"));
    }

    @Test
    void rejectsMissingBlankOrNonJsonReplies() {
        assertTrue(validate(null).isEmpty());
        assertTrue(validate("").isEmpty());
        assertTrue(validate("You both take IS442.").isEmpty());
        assertTrue(validate("{\"explanation\": \"   \"}").isEmpty());
        assertTrue(validate("{\"text\": \"You both take IS442.\"}").isEmpty());
        assertTrue(validate("{\"explanation\": 42}").isEmpty());
    }

    @Test
    void rejectsRepliesOverTheLengthLimit() {
        assertTrue(validate("{\"explanation\": \"" + "a".repeat(121) + "\"}").isEmpty());
    }

    @Test
    void rejectsSuccessClaimsAndPercentages() {
        for (String claim : new String[] {"There is a high probability you will get along.",
                "You have a good chance of success together.", "This pairing is guaranteed to work.",
                "You are 72% compatible."}) {
            assertTrue(validate("{\"explanation\": \"" + claim + "\"}").isEmpty(), claim);
        }
    }
    @Test
    void rejectsUnsupportedCourseCodesAndNumericClaims() {
        for (String text : new String[] {"Study LAW999 together.", "You have 20 hours together.",
                "You share 3 hours a week.", "You share -3.5 hours a week.", "Meet at 9 tomorrow.", "You will certainly succeed."}) {
            assertTrue(validator.validateText(text, evidence).isEmpty(), text);
        }
    }

    @Test
    void rejectsCodeStyleIdentifiersButKeepsCourseCodes() {
        for (String text : new String[] {"You both prefer ONE_TO_ONE study.", "Your EXAM_PREPARATION goal matches.",
                "This is a STRONG match overall.", "Your STUDY_MODE lines up."}) {
            assertTrue(validator.validateText(text, evidence).isEmpty(), text);
        }
        assertEquals(Optional.of("You both focus on IS442."), validator.validateText("You both focus on IS442.", evidence));
    }

    @Test
    void acceptsVariedWordingAndOnlyChecksConcreteReferences() {
        for (String text : new String[] {"Your study goals overlap, with room to compare meeting preferences.",
                "IS442 is in focus; you have 3.5 hours per week to work together.",
                "You have 3.50 free hours each week and compatible online preferences."}) {
            assertEquals(Optional.of(text), validator.validateText(text, evidence));
        }
    }

    private Optional<String> validate(String raw) {
        return validator.validate(raw, evidence);
    }
}
