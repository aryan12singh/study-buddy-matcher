package com.studybuddy.quiz;

import java.util.Map;

/**
 * A marked submission: the option chosen for each question, keyed by question ID, and how many
 * were right out of how many asked.
 */
public record QuizMark(Map<Long, Integer> chosenOptions, int score, int total) {
    public QuizMark {
        chosenOptions = Map.copyOf(chosenOptions);
    }
}
