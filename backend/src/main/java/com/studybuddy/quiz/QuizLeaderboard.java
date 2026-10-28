package com.studybuddy.quiz;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Ranks submitted attempts: higher score first, then less time taken, then whoever submitted
 * first. Every member gets a distinct rank; the student ID breaks a tie on all three so the order
 * never changes between polls.
 */
@Component
public class QuizLeaderboard {
    private static final Comparator<QuizResult> ORDER = Comparator
            .comparingInt(QuizResult::score).reversed()
            .thenComparing(QuizResult::timeTaken)
            .thenComparing(QuizResult::submittedAt)
            .thenComparing(QuizResult::studentId);

    /** A ranked result; rank 1 is first. */
    public record Ranked(int rank, QuizResult result) {
    }

    public List<Ranked> rank(List<QuizResult> results) {
        List<QuizResult> sorted = results.stream().sorted(ORDER).toList();
        return IntStream.range(0, sorted.size()).mapToObj(i -> new Ranked(i + 1, sorted.get(i))).toList();
    }

    /** The student's rank among these results, if they have submitted. */
    public Optional<Integer> rankOf(List<QuizResult> results, Long studentId) {
        return rank(results).stream()
                .filter(ranked -> ranked.result().studentId().equals(studentId))
                .map(Ranked::rank)
                .findFirst();
    }
}
