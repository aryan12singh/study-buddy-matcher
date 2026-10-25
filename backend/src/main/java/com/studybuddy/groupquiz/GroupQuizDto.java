package com.studybuddy.groupquiz;

import java.time.Instant;

/**
 * A quiz as one member sees it, without its questions. {@code status} already treats an open quiz
 * past {@code closesAt} as closed.
 *
 * @param canHost whether the signed-in member may close this quiz and start others
 */
public record GroupQuizDto(
        Long id,
        Long groupId,
        Long sessionId,
        String topics,
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
