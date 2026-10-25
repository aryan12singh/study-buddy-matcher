package com.studybuddy.groupquiz;

import com.studybuddy.common.error.InvalidInputException;
import com.studybuddy.studygroup.StudyGroup;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class QuizMarkerTest {
    private static final QuizMarker MARKER = new QuizMarker();
    private static final List<String> OPTIONS = List.of("A", "B", "C", "D");

    /** Three questions with IDs 101, 102 and 103, whose correct options are 0, 1 and 2. */
    private static List<QuizQuestion> questions() {
        var draft = new QuizDraft(List.of(
                new QuizDraft.Question("Q1", OPTIONS, 0, "E1"),
                new QuizDraft.Question("Q2", OPTIONS, 1, "E2"),
                new QuizDraft.Question("Q3", OPTIONS, 2, "E3")));
        var quiz = GroupQuiz.open(mock(StudyGroup.class), null, 7L, "Topics", draft,
                Instant.parse("2026-10-25T02:00:00Z"), Duration.ofMinutes(15));
        List<QuizQuestion> questions = quiz.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            ReflectionTestUtils.setField(questions.get(i), "id", 101L + i);
        }
        return questions;
    }

    private static QuizAnswer answer(long questionId, Integer option) {
        return new QuizAnswer(questionId, option);
    }

    @Test
    void scoresEveryAnswerCorrect() {
        QuizMark mark = MARKER.mark(questions(), List.of(answer(101, 0), answer(102, 1), answer(103, 2)));

        assertEquals(3, mark.score());
        assertEquals(3, mark.total());
        assertEquals(Map.of(101L, 0, 102L, 1, 103L, 2), mark.chosenOptions());
    }

    @Test
    void scoresNoAnswerCorrect() {
        QuizMark mark = MARKER.mark(questions(), List.of(answer(101, 3), answer(102, 3), answer(103, 3)));

        assertEquals(0, mark.score());
        assertEquals(3, mark.total());
    }

    @Test
    void acceptsAnswersInAnyOrder() {
        assertEquals(2, MARKER.mark(questions(), List.of(answer(103, 2), answer(101, 0), answer(102, 0))).score());
    }

    @Test
    void rejectsAMissingAnswer() {
        assertThrows(InvalidInputException.class, () -> MARKER.mark(questions(), List.of(answer(101, 0), answer(102, 1))));
        assertThrows(InvalidInputException.class, () -> MARKER.mark(questions(), List.of()));
        assertThrows(InvalidInputException.class, () -> MARKER.mark(questions(), null));
    }

    @Test
    void rejectsTwoAnswersToTheSameQuestion() {
        assertThrows(InvalidInputException.class, () -> MARKER.mark(questions(),
                List.of(answer(101, 0), answer(101, 1), answer(102, 1), answer(103, 2))));
    }

    @Test
    void rejectsAnAnswerToAQuestionFromAnotherQuiz() {
        assertThrows(InvalidInputException.class, () -> MARKER.mark(questions(),
                List.of(answer(101, 0), answer(102, 1), answer(999, 2))));
    }

    @Test
    void rejectsAnOptionOutsideTheFourOrMissing() {
        assertThrows(InvalidInputException.class, () -> MARKER.mark(questions(),
                List.of(answer(101, 4), answer(102, 1), answer(103, 2))));
        assertThrows(InvalidInputException.class, () -> MARKER.mark(questions(),
                List.of(answer(101, -1), answer(102, 1), answer(103, 2))));
        assertThrows(InvalidInputException.class, () -> MARKER.mark(questions(),
                List.of(answer(101, null), answer(102, 1), answer(103, 2))));
        assertThrows(InvalidInputException.class, () -> MARKER.mark(questions(),
                Arrays.asList(null, answer(102, 1), answer(103, 2))));
    }
}
