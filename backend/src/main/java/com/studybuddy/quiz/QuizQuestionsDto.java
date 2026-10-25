package com.studybuddy.quiz;

import java.time.Instant;
import java.util.List;

/**
 * The questions sent to a member who has started a quiz. Deliberately has no correct option or
 * explanation: those stay on the server until the quiz closes.
 */
public record QuizQuestionsDto(Long quizId, Instant startedAt, Instant closesAt, List<Question> questions) {

    public record Question(Long id, String prompt, List<String> options) {
    }
}
