package com.studybuddy.quiz;

import com.studybuddy.common.error.InvalidInputException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Marks a submission against a quiz's questions. Every question must be answered exactly once with
 * one of its four options; anything else is rejected as invalid input rather than scored, so a
 * member cannot skip a question to save time.
 */
@Component
public class QuizMarker {

    public QuizMark mark(List<QuizQuestion> questions, List<QuizAnswer> answers) {
        if (answers == null) {
            throw new InvalidInputException("answers", "Answer every question before submitting");
        }
        Map<Long, QuizQuestion> byId = new HashMap<>();
        for (QuizQuestion question : questions) {
            byId.put(question.getId(), question);
        }
        Map<Long, Integer> chosen = new HashMap<>();
        for (QuizAnswer answer : answers) {
            if (answer == null || !byId.containsKey(answer.questionId())) {
                throw new InvalidInputException("answers", "An answer is for a question that is not in this quiz");
            }
            if (answer.optionIndex() == null || !QuizQuestion.isOption(answer.optionIndex())) {
                throw new InvalidInputException("answers", "Each answer must choose one of the four options");
            }
            if (chosen.put(answer.questionId(), answer.optionIndex()) != null) {
                throw new InvalidInputException("answers", "Each question can be answered only once");
            }
        }
        if (chosen.size() != byId.size()) {
            throw new InvalidInputException("answers", "Answer every question before submitting");
        }
        int score = 0;
        for (QuizQuestion question : questions) {
            if (question.isCorrect(chosen.get(question.getId()))) {
                score++;
            }
        }
        return new QuizMark(chosen, score, questions.size());
    }
}
