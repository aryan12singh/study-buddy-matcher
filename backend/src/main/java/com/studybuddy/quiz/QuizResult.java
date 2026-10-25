package com.studybuddy.quiz;

import java.time.Duration;
import java.time.Instant;

/** One member's submitted attempt, as the leaderboard needs it. */
public record QuizResult(Long studentId, String displayName, int score, int total, Duration timeTaken,
        Instant submittedAt) {

    public static QuizResult of(QuizAttempt attempt) {
        return new QuizResult(attempt.getStudent().getId(), attempt.getStudent().getName(), attempt.getScore(),
                attempt.getTotal(), attempt.timeTaken(), attempt.getSubmittedAt());
    }
}
