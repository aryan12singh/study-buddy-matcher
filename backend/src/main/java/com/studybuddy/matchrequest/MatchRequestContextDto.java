package com.studybuddy.matchrequest;

import com.studybuddy.student.StudyGoal;

/**
 * The context of a match request as the API shows it: its origin and, when there was one, the
 * course and study goal it was about.
 */
public record MatchRequestContextDto(
        MatchRequestOrigin origin,
        Long courseId,
        String courseCode,
        String courseName,
        StudyGoal studyGoal) {
}
