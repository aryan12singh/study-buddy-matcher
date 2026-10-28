package com.studybuddy.quiz;

import com.studybuddy.common.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * A quiz action that the quiz's or the attempt's current state does not allow. Returned as 409 with
 * a code the screen can act on, such as showing the review once a quiz has closed.
 */
public class QuizStateConflictException extends ApiException {

    private QuizStateConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }

    public static QuizStateConflictException closed() {
        return new QuizStateConflictException("QUIZ_CLOSED", "This quiz has closed; open the review to see the answers");
    }

    public static QuizStateConflictException alreadySubmitted() {
        return new QuizStateConflictException("ALREADY_SUBMITTED", "You have already submitted this quiz");
    }

    public static QuizStateConflictException notStarted() {
        return new QuizStateConflictException("QUIZ_NOT_STARTED", "Start the quiz before submitting answers");
    }

    public static QuizStateConflictException notClosed() {
        return new QuizStateConflictException("QUIZ_NOT_CLOSED", "The answers are shown once the quiz closes");
    }
}
