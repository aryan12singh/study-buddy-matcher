package com.studybuddy.quiz;

import com.studybuddy.course.CourseDto;

import java.time.Instant;

/**
 * A quiz as one taker sees it, without its questions. {@code status} already treats an open quiz
 * past {@code closesAt} as closed. {@code groupId} and {@code sessionId} are null for a personal quiz.
 *
 * @param canHost whether the signed-in student may close this quiz, and for a group, start others
 */
public record QuizDto(
        Long id,
        QuizScope scope,
        CourseDto course,
        Long groupId,
        Long sessionId,
        String title,
        QuizStatus status,
        Long createdBy,
        Instant createdAt,
        Instant closesAt,
        Instant closedAt,
        int questionCount,
        long submittedCount,
        QuizAttemptState myAttempt,
        boolean canHost) {
}
