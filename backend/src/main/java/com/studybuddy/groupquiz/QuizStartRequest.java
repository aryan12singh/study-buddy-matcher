package com.studybuddy.groupquiz;

import com.studybuddy.course.Course;

/** A checked request to start a quiz, carried from the read step past the AI call to the save step. */
public record QuizStartRequest(Long groupId, Course course, String topics, Long sessionId) {
}
