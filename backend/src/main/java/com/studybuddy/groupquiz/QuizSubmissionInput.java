package com.studybuddy.groupquiz;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** A member's answers, one for each question. {@link QuizMarker} checks they are complete. */
public record QuizSubmissionInput(@NotNull(message = "Answer every question before submitting") List<QuizAnswer> answers) {
}
