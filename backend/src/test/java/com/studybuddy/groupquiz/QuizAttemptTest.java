package com.studybuddy.groupquiz;

import com.studybuddy.student.Student;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class QuizAttemptTest {
    private static final Instant STARTED = Instant.parse("2026-10-25T02:00:00Z");

    private static QuizAttempt started() {
        return QuizAttempt.start(mock(GroupQuiz.class), mock(Student.class), STARTED);
    }

    @Test
    void recordsTheAnswersScoreAndTimeTakenOnSubmit() {
        QuizAttempt attempt = started();
        assertFalse(attempt.isSubmitted());

        attempt.submit(Map.of(1L, 0, 2L, 3), 1, 2, STARTED.plusSeconds(95));

        assertTrue(attempt.isSubmitted());
        assertEquals(1, attempt.getScore());
        assertEquals(2, attempt.getTotal());
        assertEquals(Map.of(1L, 0, 2L, 3), attempt.getAnswers());
        assertEquals(Duration.ofSeconds(95), attempt.timeTaken());
    }

    @Test
    void canOnlyBeSubmittedOnce() {
        QuizAttempt attempt = started();
        attempt.submit(Map.of(1L, 0), 1, 1, STARTED.plusSeconds(30));

        assertThrows(IllegalStateException.class, () -> attempt.submit(Map.of(1L, 1), 0, 1, STARTED.plusSeconds(40)));
        assertEquals(1, attempt.getScore());
    }

    @Test
    void refusesAScoreOutsideTheTotalOrASubmitBeforeTheStart() {
        QuizAttempt attempt = started();

        assertThrows(IllegalArgumentException.class, () -> attempt.submit(Map.of(), 3, 2, STARTED.plusSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> attempt.submit(Map.of(), -1, 2, STARTED.plusSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> attempt.submit(Map.of(), 0, 2, STARTED.minusSeconds(1)));
        assertFalse(attempt.isSubmitted());
    }

    @Test
    void hasNoTimeTakenUntilSubmitted() {
        assertThrows(IllegalStateException.class, () -> started().timeTaken());
    }
}
