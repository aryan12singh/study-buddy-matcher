package com.studybuddy.groupquiz;

import com.studybuddy.studygroup.StudyGroup;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GroupQuizTest {
    private static final Instant STARTED = Instant.parse("2026-10-25T02:00:00Z");
    private static final Duration FIFTEEN_MINUTES = Duration.ofMinutes(15);
    private static final QuizDraft DRAFT = new QuizDraft(List.of(
            new QuizDraft.Question("What hides an object's state?", List.of("Encapsulation", "Inheritance", "Polymorphism", "Abstraction"), 0, "Encapsulation keeps fields private."),
            new QuizDraft.Question("What lets a subclass override?", List.of("Encapsulation", "Inheritance", "Polymorphism", "Composition"), 2, "Polymorphism dispatches at runtime.")));

    private static GroupQuiz openQuiz() {
        return GroupQuiz.open(mock(StudyGroup.class), null, 7L, "OOP pillars", DRAFT, STARTED, FIFTEEN_MINUTES);
    }

    @Test
    void opensWithTheDraftsQuestionsInOrderAndAClosingTime() {
        GroupQuiz quiz = openQuiz();

        assertEquals(QuizStatus.OPEN, quiz.getStatus());
        assertEquals(STARTED.plus(FIFTEEN_MINUTES), quiz.getClosesAt());
        assertEquals(7L, quiz.getCreatedBy());
        assertEquals(2, quiz.getQuestions().size());
        assertEquals(0, quiz.getQuestions().get(0).getPosition());
        assertEquals("What lets a subclass override?", quiz.getQuestions().get(1).getPrompt());
        assertTrue(quiz.getQuestions().get(1).isCorrect(2));
        assertFalse(quiz.getQuestions().get(1).isCorrect(0));
    }

    @Test
    void readsAsClosedOnceTheOpenWindowHasPassed() {
        GroupQuiz quiz = openQuiz();

        assertTrue(quiz.isOpenAt(STARTED.plus(Duration.ofMinutes(14))));
        assertEquals(QuizStatus.CLOSED, quiz.statusAt(STARTED.plus(FIFTEEN_MINUTES)));
        assertEquals(QuizStatus.OPEN, quiz.getStatus(), "Nothing is stored until someone closes it");
    }

    @Test
    void closingEarlyRecordsWhenItClosed() {
        GroupQuiz quiz = openQuiz();
        Instant early = STARTED.plus(Duration.ofMinutes(5));

        quiz.close(early);

        assertEquals(QuizStatus.CLOSED, quiz.getStatus());
        assertEquals(early, quiz.getClosedAt());
        assertEquals(early, quiz.endedAt());
        assertFalse(quiz.isOpenAt(early.plusSeconds(1)));
    }

    @Test
    void closingAfterTheWindowKeepsTheWindowsEndAsTheEndTime() {
        GroupQuiz quiz = openQuiz();

        quiz.close(STARTED.plus(Duration.ofHours(1)));

        assertEquals(QuizStatus.CLOSED, quiz.getStatus());
        assertNull(quiz.getClosedAt());
        assertEquals(quiz.getClosesAt(), quiz.endedAt());
    }

    @Test
    void closingTwiceKeepsTheFirstClosingTime() {
        GroupQuiz quiz = openQuiz();
        Instant first = STARTED.plus(Duration.ofMinutes(5));

        quiz.close(first);
        quiz.close(first.plusSeconds(30));

        assertEquals(first, quiz.getClosedAt());
    }

    @Test
    void refusesAnEmptyDraftOrNoOpenTime() {
        assertThrows(IllegalArgumentException.class, () -> GroupQuiz.open(mock(StudyGroup.class), null, 7L, "OOP",
                new QuizDraft(List.of()), STARTED, FIFTEEN_MINUTES));
        assertThrows(IllegalArgumentException.class, () -> GroupQuiz.open(mock(StudyGroup.class), null, 7L, "OOP",
                DRAFT, STARTED, Duration.ZERO));
    }

    @Test
    void refusesAQuestionWithoutFourOptionsOrWithAnAnswerOutsideThem() {
        var threeOptions = new QuizDraft(List.of(new QuizDraft.Question("Q", List.of("A", "B", "C"), 0, "E")));
        var answerOutside = new QuizDraft(List.of(new QuizDraft.Question("Q", List.of("A", "B", "C", "D"), 4, "E")));

        assertThrows(IllegalArgumentException.class, () -> GroupQuiz.open(mock(StudyGroup.class), null, 7L, "OOP",
                threeOptions, STARTED, FIFTEEN_MINUTES));
        assertThrows(IllegalArgumentException.class, () -> GroupQuiz.open(mock(StudyGroup.class), null, 7L, "OOP",
                answerOutside, STARTED, FIFTEEN_MINUTES));
    }
}
