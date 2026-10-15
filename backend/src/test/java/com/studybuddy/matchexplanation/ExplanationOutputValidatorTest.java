package com.studybuddy.matchexplanation;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplanationOutputValidatorTest {
    private final ExplanationOutputValidator validator = new ExplanationOutputValidator(120, ExplanationFixtures.JSON);

    @Test
    void acceptsAShortGroundedExplanationAndTrimsIt() {
        assertEquals(Optional.of("You both take IS442 and share 3.5 free hours a week."),
                validator.validate("{\"explanation\": \"  You both take IS442 and share 3.5 free hours a week. \"}"));
    }

    @Test
    void rejectsMissingBlankOrNonJsonReplies() {
        assertTrue(validator.validate(null).isEmpty());
        assertTrue(validator.validate("").isEmpty());
        assertTrue(validator.validate("You both take IS442.").isEmpty());
        assertTrue(validator.validate("{\"explanation\": \"   \"}").isEmpty());
        assertTrue(validator.validate("{\"text\": \"You both take IS442.\"}").isEmpty());
        assertTrue(validator.validate("{\"explanation\": 42}").isEmpty());
    }

    @Test
    void rejectsRepliesOverTheLengthLimit() {
        assertTrue(validator.validate("{\"explanation\": \"" + "a".repeat(121) + "\"}").isEmpty());
    }

    @Test
    void rejectsSuccessClaimsAndPercentages() {
        for (String claim : new String[] {"There is a high probability you will get along.",
                "You have a good chance of success together.", "This pairing is guaranteed to work.",
                "You are 72% compatible."}) {
            assertTrue(validator.validate("{\"explanation\": \"" + claim + "\"}").isEmpty(), claim);
        }
    }
}
