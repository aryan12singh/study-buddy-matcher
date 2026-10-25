package com.studybuddy.groupquiz;

import java.util.List;

/**
 * Questions Gemini wrote that have passed the quiz output validator. Not saved until the service
 * turns them into a {@link GroupQuiz}.
 */
public record QuizDraft(List<Question> questions) {

    public QuizDraft {
        questions = List.copyOf(questions);
    }

    /** One multiple-choice question; {@code correctOption} indexes {@code options}. */
    public record Question(String prompt, List<String> options, int correctOption, String explanation) {
        public Question {
            options = List.copyOf(options);
        }
    }
}
