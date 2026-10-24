package com.studybuddy.studyplan;

import com.studybuddy.course.CourseDto;
import com.studybuddy.student.StudyGoal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** A whole plan with its entries. Holds no student's name, email or contact number. */
public record StudyPlanDto(
        Long id,
        StudyPlanScope scope,
        CourseDto course,
        Long sessionId,
        LocalDate firstBlockDate,
        int blockCount,
        int minutesPerBlock,
        LocalDate examDate,
        StudyGoal studyGoal,
        StudyMaterialSource sourceKind,
        List<String> sourceFilenames,
        List<String> sourceTopics,
        boolean canManage,
        List<StudyPlanEntryDto> entries,
        Instant createdAt,
        Instant updatedAt) {
}
