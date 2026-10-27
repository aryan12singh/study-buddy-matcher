package com.studybuddy.quiz;

import com.studybuddy.common.error.NotFoundException;

/** Also used for a quiz the student may not see, so a quiz's ID reveals nothing about it. */
public class QuizNotFoundException extends NotFoundException {
    public QuizNotFoundException(Long quizId) {
        super("Quiz " + quizId + " not found");
    }
}
