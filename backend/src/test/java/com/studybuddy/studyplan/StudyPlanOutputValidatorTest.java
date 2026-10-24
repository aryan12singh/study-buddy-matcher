package com.studybuddy.studyplan;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudyPlanOutputValidatorTest {
    /** At most 3 topics, 2 entries per block and 2 tasks per entry, so each limit is easy to cross. */
    private static final StudyPlanOutputValidator VALIDATOR = new StudyPlanOutputValidator(
            new StudyPlanProperties(DataSize.ofMegabytes(10), 6, 4000, 100000, 3, 2, 2, 16, 40, "Asia/Singapore"), JsonMapper.builder().build());
    /** Two weeks of two hours each. */
    private static final RoadmapFrame TWO_WEEKS = RoadmapFrame.weeks(LocalDate.of(2026, 10, 23), 2, 120);

    private static Optional<RoadmapDraft> validate(String reply) {
        return VALIDATOR.validate(reply, TWO_WEEKS);
    }

    private static String reply(String topics, String entries) {
        return "{\"sourceTopics\": " + topics + ", \"entries\": " + entries + "}";
    }

    private static String entry(Object sequence, String title, Object minutes, String tasks) {
        return "{\"sequence\": " + sequence + ", \"title\": " + title + ", \"plannedMinutes\": " + minutes
                + ", \"tasks\": " + tasks + "}";
    }

    private static final String TOPICS = "[\"Encapsulation\", \"Polymorphism\"]";
    private static final String WEEK_ONE = entry(1, "\"Encapsulation\"", 120, "[\"Re-read lecture 2\"]");
    private static final String WEEK_TWO = entry(2, "\"Polymorphism\"", 90, "[]");

    private static String entries(String... entries) {
        return "[" + String.join(", ", entries) + "]";
    }

    @Test
    void acceptsAPlanThatFitsTheFrame() {
        RoadmapDraft draft = validate(reply(TOPICS, entries(WEEK_ONE, WEEK_TWO))).orElseThrow();

        assertEquals(List.of("Encapsulation", "Polymorphism"), draft.sourceTopics());
        assertEquals(new RoadmapDraft.Entry(1, "Encapsulation", 120, List.of("Re-read lecture 2")), draft.entries().getFirst());
        assertEquals(new RoadmapDraft.Entry(2, "Polymorphism", 90, List.of()), draft.entries().get(1));
    }

    @Test
    void sortsEntriesByBlockAndKeepsTheirOrderWithinABlock() {
        String later = entry(1, "\"Inheritance\"", 30, "[]");
        String earlier = entry(1, "\"Encapsulation\"", 60, "[]");

        RoadmapDraft draft = validate(reply(TOPICS, entries(WEEK_TWO, earlier, later))).orElseThrow();

        assertEquals(List.of("Encapsulation", "Inheritance", "Polymorphism"),
                draft.entries().stream().map(RoadmapDraft.Entry::title).toList());
    }

    @Test
    void trimsTextAndDropsRepeatedTopics() {
        String topics = "[\" Encapsulation \", \"encapsulation\", \"Polymorphism\"]";

        RoadmapDraft draft = validate(reply(topics, entries(WEEK_ONE, WEEK_TWO))).orElseThrow();

        assertEquals(List.of("Encapsulation", "Polymorphism"), draft.sourceTopics());
    }

    @Test
    void acceptsWholeNumbersWrittenWithADecimalPointAndAMissingTaskList() {
        String noTasks = "{\"sequence\": 2.0, \"title\": \"Polymorphism\", \"plannedMinutes\": 90.0}";

        assertTrue(validate(reply(TOPICS, entries(WEEK_ONE, noTasks))).isPresent());
    }

    @Test
    void ignoresFieldsItDoesNotUse() {
        String extra = "{\"sourceTopics\": " + TOPICS + ", \"entries\": " + entries(WEEK_ONE, WEEK_TWO) + ", \"note\": \"hi\"}";

        assertTrue(validate(extra).isPresent());
    }

    @Test
    void rejectsAReplyThatIsNotTheAgreedJson() {
        assertTrue(validate(null).isEmpty());
        assertTrue(validate("").isEmpty());
        assertTrue(validate("Here is your plan!").isEmpty());
        assertTrue(validate("[1, 2]").isEmpty());
        assertTrue(validate("{\"sourceTopics\": " + TOPICS + "}").isEmpty());
    }

    @Test
    void rejectsMissingEmptyOrTooManyTopics() {
        assertTrue(validate(reply("[]", entries(WEEK_ONE, WEEK_TWO))).isEmpty());
        assertTrue(validate(reply("\"Encapsulation\"", entries(WEEK_ONE, WEEK_TWO))).isEmpty());
        assertTrue(validate(reply("[\"A\", \"B\", \"C\", \"D\"]", entries(WEEK_ONE, WEEK_TWO))).isEmpty());
        assertTrue(validate(reply("[\"Encapsulation\", \"  \"]", entries(WEEK_ONE, WEEK_TWO))).isEmpty());
        assertTrue(validate(reply("[\"Encapsulation\", 7]", entries(WEEK_ONE, WEEK_TWO))).isEmpty());
    }

    @Test
    void rejectsAnEntryOutsideTheFrame() {
        String weekThree = entry(3, "\"Generics\"", 60, "[]");
        String weekZero = entry(0, "\"Generics\"", 60, "[]");

        assertTrue(validate(reply(TOPICS, entries(WEEK_ONE, WEEK_TWO, weekThree))).isEmpty());
        assertTrue(validate(reply(TOPICS, entries(WEEK_ONE, WEEK_TWO, weekZero))).isEmpty());
    }

    @Test
    void rejectsAPlanThatLeavesABlockEmpty() {
        assertTrue(validate(reply(TOPICS, entries(WEEK_ONE))).isEmpty());
    }

    @Test
    void rejectsABlockWhoseMinutesAddUpToMoreThanItsLimit() {
        String overflow = entry(1, "\"Inheritance\"", 1, "[]");

        assertTrue(validate(reply(TOPICS, entries(WEEK_ONE, overflow, WEEK_TWO))).isEmpty());
    }

    @Test
    void rejectsTooManyEntriesInOneBlock() {
        String a = entry(2, "\"A\"", 10, "[]");
        String b = entry(2, "\"B\"", 10, "[]");

        assertTrue(validate(reply(TOPICS, entries(WEEK_ONE, WEEK_TWO, a, b))).isEmpty());
    }

    @Test
    void rejectsAnEntryWithBadFields() {
        String[] bad = {
                entry("\"1\"", "\"Encapsulation\"", 60, "[]"),
                entry(1.5, "\"Encapsulation\"", 60, "[]"),
                entry(1, "\"  \"", 60, "[]"),
                entry(1, "null", 60, "[]"),
                entry(1, "\"" + "x".repeat(256) + "\"", 60, "[]"),
                entry(1, "\"Encapsulation\"", 0, "[]"),
                entry(1, "\"Encapsulation\"", -30, "[]"),
                entry(1, "\"Encapsulation\"", "\"60\"", "[]"),
                entry(1, "\"Encapsulation\"", 99999999999L, "[]"),
                entry(1, "\"Encapsulation\"", 60, "\"Read the notes\""),
                entry(1, "\"Encapsulation\"", 60, "[\"One\", \"Two\", \"Three\"]"),
                entry(1, "\"Encapsulation\"", 60, "[\"\"]")
        };
        for (String entry : bad) {
            assertTrue(validate(reply(TOPICS, entries(entry, WEEK_TWO))).isEmpty(), entry);
        }
    }

    @Test
    void checksMeetingsTheSameWayAsWeeks() {
        RoadmapFrame oneMeeting = RoadmapFrame.meetings(List.of(LocalDate.of(2026, 10, 29)), 90);
        String agenda = entries(entry(1, "\"Warm-up quiz\"", 15, "[]"), entry(1, "\"Polymorphism\"", 75, "[]"));
        String tooLong = entries(entry(1, "\"Warm-up quiz\"", 15, "[]"), entry(1, "\"Polymorphism\"", 76, "[]"));

        assertTrue(VALIDATOR.validate(reply(TOPICS, agenda), oneMeeting).isPresent());
        assertTrue(VALIDATOR.validate(reply(TOPICS, tooLong), oneMeeting).isEmpty());
    }
}
