package com.studybuddy.quiz;

/** A member's own result right after submitting; the answers are revealed only once the quiz closes. */
public record AttemptResultDto(int score, int total, long timeTakenSeconds, int rank) {
}
