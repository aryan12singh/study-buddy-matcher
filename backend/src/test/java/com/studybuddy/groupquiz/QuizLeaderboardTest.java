package com.studybuddy.groupquiz;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizLeaderboardTest {
    private static final QuizLeaderboard LEADERBOARD = new QuizLeaderboard();
    private static final Instant OPENED = Instant.parse("2026-10-25T02:00:00Z");

    private static QuizResult result(long studentId, int score, int secondsTaken, int secondsAfterOpening) {
        return new QuizResult(studentId, "Student " + studentId, score, 10, Duration.ofSeconds(secondsTaken),
                OPENED.plusSeconds(secondsAfterOpening));
    }

    private static List<Long> order(List<QuizResult> results) {
        return LEADERBOARD.rank(results).stream().map(ranked -> ranked.result().studentId()).toList();
    }

    @Test
    void ranksHigherScoresFirst() {
        var ranked = LEADERBOARD.rank(List.of(result(1, 6, 60, 60), result(2, 9, 300, 300), result(3, 8, 30, 30)));

        assertEquals(List.of(2L, 3L, 1L), ranked.stream().map(r -> r.result().studentId()).toList());
        assertEquals(List.of(1, 2, 3), ranked.stream().map(QuizLeaderboard.Ranked::rank).toList());
    }

    @Test
    void breaksAnEqualScoreByLessTimeTaken() {
        assertEquals(List.of(2L, 1L), order(List.of(result(1, 8, 120, 130), result(2, 8, 90, 200))));
    }

    @Test
    void breaksAnEqualScoreAndTimeByWhoSubmittedFirst() {
        assertEquals(List.of(2L, 1L), order(List.of(result(1, 8, 90, 200), result(2, 8, 90, 150))));
    }

    @Test
    void givesAFullTieAStableOrder() {
        assertEquals(List.of(1L, 2L), order(List.of(result(2, 8, 90, 150), result(1, 8, 90, 150))));
    }

    @Test
    void isEmptyBeforeAnyoneSubmits() {
        assertTrue(LEADERBOARD.rank(List.of()).isEmpty());
        assertEquals(Optional.empty(), LEADERBOARD.rankOf(List.of(), 1L));
    }

    @Test
    void findsOneStudentsRankOrNothingIfTheyHaveNotSubmitted() {
        var results = List.of(result(1, 6, 60, 60), result(2, 9, 300, 300), result(3, 8, 30, 30));

        assertEquals(Optional.of(3), LEADERBOARD.rankOf(results, 1L));
        assertEquals(Optional.of(1), LEADERBOARD.rankOf(results, 2L));
        assertEquals(Optional.empty(), LEADERBOARD.rankOf(results, 4L));
    }
}
