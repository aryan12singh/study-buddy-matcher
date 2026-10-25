package com.studybuddy.groupquiz;

import com.studybuddy.common.error.NotFoundException;

/** Also used for a quiz the student may not see, so a quiz's ID reveals nothing about it. */
public class GroupQuizNotFoundException extends NotFoundException {
    public GroupQuizNotFoundException(Long quizId) {
        super("Quiz " + quizId + " not found");
    }
}
