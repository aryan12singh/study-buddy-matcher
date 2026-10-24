package com.studybuddy.studyplan;

import com.studybuddy.student.StudyGoal;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * The form fields sent with a personal plan's files, as multipart form fields so a browser form or
 * Postman can send them beside the files. The material is either the files or {@code topicsText}.
 *
 * @param examDate the exam to plan up to; without one the plan covers this week alone
 */
public record PersonalPlanInput(
        @NotNull Long courseId,
        @NotNull Integer hoursPerWeek,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate examDate,
        StudyGoal studyGoal,
        String topicsText) {
}
