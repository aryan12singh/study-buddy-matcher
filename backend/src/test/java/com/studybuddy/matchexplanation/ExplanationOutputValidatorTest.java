package com.studybuddy.matchexplanation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @ValueSource(strings = {"IS442", "IS212", "IS210", "IS214", "IS216", "IS111", "IS112", "IS113", "IS114", "IS115",
            "COR-STAT1202", "ACCT101", "ECON101", "FNCE101"})
    void acceptsEverySeededCourseCodeWhenItIsTheEvidenceCourse(String code) {
        String text = "You both focus on " + code + " and share about 3.5 hours per week.";

        assertEquals(Optional.of(text), validator.validateText(text, withCourse(code)));
    }

    @Test
    void acceptsOrdinaryAcronymsThatAreNotCodeNames() {
        String text = "Your OOP and SQL course focus lines up well.";

        assertEquals(Optional.of(text), validator.validateText(text, evidence));
    }

    @Test
    void rejectsSoftenedSuccessClaims() {
        for (String text : new String[] {"This would be a successful pairing.", "You are likely to work well together.",
                "There is a decent chance you will click."}) {
            assertTrue(validator.validateText(text, evidence).isEmpty(), text);
        }
    }

    @Test
    void checksSpelledOutHoursAgainstTheEvidence() {
        assertTrue(validator.validateText("You share ten hours a week.", evidence).isEmpty());
        String twoHours = "You share about two hours a week.";

        assertEquals(Optional.of(twoHours), validator.validateText(twoHours, withHours(2.0)));
    }

    @Test
    void acceptsHyphenatedHourCounts() {
        String text = "You have a 3.5-hour weekly overlap.";

        assertEquals(Optional.of(text), validator.validateText(text, evidence));
    }

    /**
     * Real gemini-3.5-flash replies to prompt v4 (18 October), for evidence with IS442 and 2 shared
     * hours. A change to the checks must keep accepting good provider wording.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "You and this student match strongly on course for IS442 Object Oriented Programming, meeting mode, and group preference. Your lowest matching score is for availability. You both share the study goal of exam preparation, and about two hours per week of shared time.",
        "your strongest matches are course focus for IS442, meeting mode, and group preference for one to one study. study goals align on exam preparation, while your weakest match is availability with about 2 hours per week shared.",
        "your strongest matches with this student are meeting mode, group preference, and course alignment for IS442 Object Oriented Programming, with shared goals including exam preparation and about 2 shared hours per week. your weakest match is availability."})
    void acceptsRecordedLiveProviderReplies(String reply) {
        ExplanationOutputValidator generous = new ExplanationOutputValidator(600, ExplanationFixtures.JSON);

        assertEquals(Optional.of(reply), generous.validateText(reply, withHours(2.0)));
    }

    private ExplanationEvidence withCourse(String code) {
        CourseEvidence course = new CourseEvidence(code, "A course");
        return new ExplanationEvidence(evidence.quality(), evidence.totalScore(), evidence.strategy(), evidence.criteria(),
                evidence.sharedHoursPerWeek(), course, course, evidence.searchedGoal(), evidence.sharedGoals(),
                evidence.searcherStudyMode(), evidence.candidateStudyMode(), evidence.searcherGroupSize(),
                evidence.candidateGroupSize());
    }

    private ExplanationEvidence withHours(double hours) {
        return new ExplanationEvidence(evidence.quality(), evidence.totalScore(), evidence.strategy(), evidence.criteria(),
                hours, evidence.scoringCourse(), evidence.sharedCourse(), evidence.searchedGoal(), evidence.sharedGoals(),
                evidence.searcherStudyMode(), evidence.candidateStudyMode(), evidence.searcherGroupSize(),
                evidence.candidateGroupSize());
    }

    private Optional<String> validate(String raw) {
        return validator.validate(raw, evidence);
    }
}
