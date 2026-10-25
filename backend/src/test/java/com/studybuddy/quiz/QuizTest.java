package com.studybuddy.quiz;

import com.studybuddy.course.Course;
import com.studybuddy.student.Student;
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
import static org.mockito.Mockito.when;

class QuizTest {
    private static final Instant STARTED = Instant.parse("2026-10-25T02:00:00Z");
    private static final Duration FIFTEEN_MINUTES = Duration.ofMinutes(15);
    private static final QuizDraft DRAFT = new QuizDraft(List.of(
            new QuizDraft.Question("What hides an object's state?", List.of("Encapsulation", "Inheritance", "Polymorphism", "Abstraction"), 0, "Encapsulation keeps fields private."),
            new QuizDraft.Question("What lets a subclass override?", List.of("Encapsulation", "Inheritance", "Polymorphism", "Composition"), 2, "Polymorphism dispatches at runtime.")));

    private static final Course IS442 = new Course("IS442", "Object Oriented Programming");

    private static StudyGroup group() {
        StudyGroup group = mock(StudyGroup.class);
        when(group.getCourse()).thenReturn(IS442);
        return group;
    }

    private static Student student(long id) {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(id);
        return student;
    }

    private static Quiz openQuiz() {
        return Quiz.forGroup(group(), null, 7L, "OOP pillars", DRAFT, STARTED, FIFTEEN_MINUTES);
    }

    private static Quiz personalQuiz() {
        return Quiz.personal(student(11L), IS442, "OOP pillars", DRAFT, STARTED, FIFTEEN_MINUTES);
    }

    @Test
    void opensWithTheDraftsQuestionsInOrderAndAClosingTime() {
        Quiz quiz = openQuiz();

        assertEquals(QuizScope.GROUP, quiz.getScope());
        assertEquals(IS442, quiz.getCourse(), "A group quiz is on the group's course");
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
        Quiz quiz = openQuiz();

        assertTrue(quiz.isOpenAt(STARTED.plus(Duration.ofMinutes(14))));
        assertEquals(QuizStatus.CLOSED, quiz.statusAt(STARTED.plus(FIFTEEN_MINUTES)));
        assertEquals(QuizStatus.OPEN, quiz.getStatus(), "Nothing is stored until someone closes it");
    }

    @Test
    void closingEarlyRecordsWhenItClosed() {
        Quiz quiz = openQuiz();
        Instant early = STARTED.plus(Duration.ofMinutes(5));

        quiz.close(early);

        assertEquals(QuizStatus.CLOSED, quiz.getStatus());
        assertEquals(early, quiz.getClosedAt());
        assertEquals(early, quiz.endedAt());
        assertFalse(quiz.isOpenAt(early.plusSeconds(1)));
    }

    @Test
    void closingAfterTheWindowKeepsTheWindowsEndAsTheEndTime() {
        Quiz quiz = openQuiz();

        quiz.close(STARTED.plus(Duration.ofHours(1)));

        assertEquals(QuizStatus.CLOSED, quiz.getStatus());
        assertNull(quiz.getClosedAt());
        assertEquals(quiz.getClosesAt(), quiz.endedAt());
    }

    @Test
    void closingTwiceKeepsTheFirstClosingTime() {
        Quiz quiz = openQuiz();
        Instant first = STARTED.plus(Duration.ofMinutes(5));

        quiz.close(first);
        quiz.close(first.plusSeconds(30));

        assertEquals(first, quiz.getClosedAt());
    }

    @Test
    void refusesAnEmptyDraftOrNoOpenTime() {
        assertThrows(IllegalArgumentException.class, () -> Quiz.forGroup(group(), null, 7L, "OOP",
                new QuizDraft(List.of()), STARTED, FIFTEEN_MINUTES));
        assertThrows(IllegalArgumentException.class, () -> Quiz.forGroup(group(), null, 7L, "OOP",
                DRAFT, STARTED, Duration.ZERO));
    }

    @Test
    void refusesAQuestionWithoutFourOptionsOrWithAnAnswerOutsideThem() {
        var threeOptions = new QuizDraft(List.of(new QuizDraft.Question("Q", List.of("A", "B", "C"), 0, "E")));
        var answerOutside = new QuizDraft(List.of(new QuizDraft.Question("Q", List.of("A", "B", "C", "D"), 4, "E")));

        assertThrows(IllegalArgumentException.class, () -> Quiz.forGroup(group(), null, 7L, "OOP",
                threeOptions, STARTED, FIFTEEN_MINUTES));
        assertThrows(IllegalArgumentException.class, () -> Quiz.forGroup(group(), null, 7L, "OOP",
                answerOutside, STARTED, FIFTEEN_MINUTES));
    }

    @Test
    void aPersonalQuizBelongsToItsStudentOnly() {
        Quiz quiz = personalQuiz();

        assertEquals(QuizScope.PERSONAL, quiz.getScope());
        assertEquals(11L, quiz.getCreatedBy());
        assertNull(quiz.getStudyGroup());
        assertTrue(quiz.isOwnedBy(11L));
        assertFalse(quiz.isOwnedBy(12L));
        assertFalse(openQuiz().isOwnedBy(7L), "Nobody owns a group quiz");
    }

    @Test
    void aPersonalQuizClosesOnceItsOwnerSubmitsButAGroupQuizStaysOpen() {
        Quiz personal = personalQuiz();
        Quiz group = openQuiz();
        Instant submitted = STARTED.plus(Duration.ofMinutes(4));

        personal.afterSubmission(submitted);
        group.afterSubmission(submitted);

        assertEquals(QuizStatus.CLOSED, personal.getStatus());
        assertEquals(submitted, personal.getClosedAt());
        assertEquals(QuizStatus.OPEN, group.getStatus());
    }
}
