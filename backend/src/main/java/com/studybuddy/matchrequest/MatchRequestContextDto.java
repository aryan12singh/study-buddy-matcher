package com.studybuddy.matchrequest;

import com.studybuddy.student.StudyGoal;

public record MatchRequestContextDto(
        MatchRequestOrigin origin,
        Long courseId,
        String courseCode,
        String courseName,
        StudyGoal studyGoal) {
}
