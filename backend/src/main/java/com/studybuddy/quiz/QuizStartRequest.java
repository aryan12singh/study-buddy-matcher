package com.studybuddy.quiz;

import com.studybuddy.course.Course;

/**
 * A checked request to start a quiz, carried from the read step past the AI call to the save step.
 * {@code groupId} and {@code sessionId} are null for a personal quiz.
 *
 * @param focus what to ask mostly about, or null to cover the whole material
 */
public record QuizStartRequest(Long groupId, Course course, String focus, Long sessionId) {
}
