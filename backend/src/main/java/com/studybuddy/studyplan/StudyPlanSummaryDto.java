package com.studybuddy.studyplan;

import com.studybuddy.course.CourseDto;
import com.studybuddy.student.StudyGoal;

import java.time.Instant;
import java.time.LocalDate;

/** One line of the student's plan list, with progress counts instead of the entries. */
public record StudyPlanSummaryDto(
        Long id,
        StudyPlanScope scope,
        CourseDto course,
        LocalDate firstBlockDate,
        int blockCount,
        LocalDate examDate,
        StudyGoal studyGoal,
        int entryCount,
        int completedCount,
        Instant createdAt) {
}
