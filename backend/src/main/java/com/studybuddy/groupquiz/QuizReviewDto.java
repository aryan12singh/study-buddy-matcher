package com.studybuddy.groupquiz;

import java.util.List;

/**
 * A closed quiz's answers, with the signed-in member's own choices. Score, total and chosen options
 * are null for a member who did not submit.
 */
public record QuizReviewDto(Long quizId, Integer score, Integer total, List<Question> questions) {

    public record Question(Long id, String prompt, List<String> options, int correctOption, Integer chosenOption,
            String explanation) {
    }
}
