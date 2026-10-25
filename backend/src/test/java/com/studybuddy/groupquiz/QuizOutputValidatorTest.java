package com.studybuddy.groupquiz;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizOutputValidatorTest {
    /** Two questions per quiz, so a wrong count is easy to write. */
    private static final QuizOutputValidator VALIDATOR = new QuizOutputValidator(
            new QuizProperties(2, Duration.ofMinutes(15), 1000), JsonMapper.builder().build());

    private static Optional<QuizDraft> validate(String reply) {
        return VALIDATOR.validate(reply);
    }

    private static String reply(String... questions) {
        return "{\"questions\": [" + String.join(", ", questions) + "]}";
    }

    private static String question(String prompt, String options, Object correct, String explanation) {
        return "{\"prompt\": " + prompt + ", \"options\": " + options + ", \"correctOption\": " + correct
                + ", \"explanation\": " + explanation + "}";
    }

    private static final String OPTIONS = "[\"Encapsulation\", \"Inheritance\", \"Polymorphism\", \"Abstraction\"]";
    private static final String FIRST = question("\"What hides state?\"", OPTIONS, 0, "\"Private fields.\"");
    private static final String SECOND = question("\"What overrides at runtime?\"", OPTIONS, 2, "\"Dynamic dispatch.\"");

    private static String quoted(int length) {
        return "\"" + "x".repeat(length) + "\"";
    }

    @Test
    void acceptsAWellFormedReplyAndTrimsItsText() {
        var draft = validate(reply(question("\"  What hides state?  \"", OPTIONS, 0, "\" Private fields. \""), SECOND));

        assertTrue(draft.isPresent());
        assertEquals(2, draft.get().questions().size());
        var first = draft.get().questions().get(0);
        assertEquals("What hides state?", first.prompt());
        assertEquals("Private fields.", first.explanation());
        assertEquals(4, first.options().size());
        assertEquals(2, draft.get().questions().get(1).correctOption());
    }

    @Test
    void acceptsACorrectOptionWrittenAsAWholeDecimal() {
        assertTrue(validate(reply(FIRST, question("\"Q\"", OPTIONS, "2.0", "\"E\""))).isPresent());
    }

    @Test
    void rejectsSomethingThatIsNotTheAgreedJson() {
        assertTrue(validate(null).isEmpty());
        assertTrue(validate("").isEmpty());
        assertTrue(validate("Sure! Here is your quiz:").isEmpty());
        assertTrue(validate("{\"questions\": {}}").isEmpty());
        assertTrue(validate("[]").isEmpty());
    }

    @Test
    void rejectsTooFewOrTooManyQuestions() {
        assertTrue(validate(reply(FIRST)).isEmpty());
        assertTrue(validate(reply(FIRST, SECOND, FIRST)).isEmpty());
        assertTrue(validate(reply()).isEmpty());
    }

    @Test
    void rejectsAQuestionWithoutExactlyFourDistinctOptions() {
        assertTrue(validate(reply(FIRST, question("\"Q\"", "[\"A\", \"B\", \"C\"]", 0, "\"E\""))).isEmpty());
        assertTrue(validate(reply(FIRST, question("\"Q\"", "[\"A\", \"B\", \"C\", \"D\", \"E\"]", 0, "\"E\""))).isEmpty());
        assertTrue(validate(reply(FIRST, question("\"Q\"", "[\"A\", \"B\", \"C\", \"a\"]", 0, "\"E\""))).isEmpty());
        assertTrue(validate(reply(FIRST, question("\"Q\"", "[\"A\", \"B\", \"C\", \"  \"]", 0, "\"E\""))).isEmpty());
        assertTrue(validate(reply(FIRST, question("\"Q\"", "[\"A\", \"B\", \"C\", 4]", 0, "\"E\""))).isEmpty());
    }

    @Test
    void rejectsACorrectOptionOutsideTheFourOrNotAWholeNumber() {
        assertTrue(validate(reply(FIRST, question("\"Q\"", OPTIONS, 4, "\"E\""))).isEmpty());
        assertTrue(validate(reply(FIRST, question("\"Q\"", OPTIONS, -1, "\"E\""))).isEmpty());
        assertTrue(validate(reply(FIRST, question("\"Q\"", OPTIONS, "1.5", "\"E\""))).isEmpty());
        assertTrue(validate(reply(FIRST, question("\"Q\"", OPTIONS, "\"1\"", "\"E\""))).isEmpty());
        assertTrue(validate(reply(FIRST, question("\"Q\"", OPTIONS, "null", "\"E\""))).isEmpty());
    }

    @Test
    void rejectsAMissingOrBlankPromptOrExplanation() {
        assertTrue(validate(reply(FIRST, question("\"  \"", OPTIONS, 0, "\"E\""))).isEmpty());
        assertTrue(validate(reply(FIRST, question("null", OPTIONS, 0, "\"E\""))).isEmpty());
        assertTrue(validate(reply(FIRST, question("\"Q\"", OPTIONS, 0, "\"\""))).isEmpty());
        assertTrue(validate(reply(FIRST, "{\"prompt\": \"Q\", \"options\": " + OPTIONS + ", \"correctOption\": 0}")).isEmpty());
    }

    @Test
    void rejectsTextLongerThanItsColumn() {
        String longOption = "[" + quoted(QuizQuestion.OPTION_LIMIT + 1) + ", \"B\", \"C\", \"D\"]";

        assertTrue(validate(reply(FIRST, question(quoted(QuizQuestion.PROMPT_LIMIT), OPTIONS, 0, "\"E\""))).isPresent());
        assertTrue(validate(reply(FIRST, question(quoted(QuizQuestion.PROMPT_LIMIT + 1), OPTIONS, 0, "\"E\""))).isEmpty());
        assertTrue(validate(reply(FIRST, question("\"Q\"", longOption, 0, "\"E\""))).isEmpty());
        assertTrue(validate(reply(FIRST, question("\"Q\"", OPTIONS, 0, quoted(QuizQuestion.EXPLANATION_LIMIT + 1)))).isEmpty());
    }

    @Test
    void acceptsTheConfiguredCountWhateverItIs() {
        var tenQuestions = new QuizOutputValidator(new QuizProperties(10, Duration.ofMinutes(15), 1000), JsonMapper.builder().build());

        assertTrue(tenQuestions.validate(reply(Collections.nCopies(10, FIRST).toArray(String[]::new))).isPresent());
        assertTrue(tenQuestions.validate(reply(FIRST, SECOND)).isEmpty());
    }
}
