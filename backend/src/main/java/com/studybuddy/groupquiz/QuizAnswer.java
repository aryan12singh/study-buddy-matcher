package com.studybuddy.groupquiz;

/** The option a member chose for one question, as submitted. */
public record QuizAnswer(Long questionId, Integer optionIndex) {
}
