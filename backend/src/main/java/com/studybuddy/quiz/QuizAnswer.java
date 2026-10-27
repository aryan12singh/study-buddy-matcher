package com.studybuddy.quiz;

/** The option a member chose for one question, as submitted. */
public record QuizAnswer(Long questionId, Integer optionIndex) {
}
