package com.studybuddy.quiz;

import java.util.List;

/**
 * The current members who have submitted, ranked. Holds display names and scores only, never an
 * email or contact number.
 */
public record LeaderboardDto(Long quizId, QuizStatus status, long submittedCount, long notSubmittedCount, List<Entry> entries) {

    /** @param me whether this row is the signed-in member's, so the screen can highlight it */
    public record Entry(int rank, Long studentId, String displayName, int score, int total, long timeTakenSeconds, boolean me) {
    }
}
