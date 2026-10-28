package com.studybuddy.studyplan;

import com.studybuddy.student.StudyGoal;

/**
 * The form fields sent with a session plan's files, as multipart form fields like a personal plan's.
 * The meetings and their length come from the session itself. The material is either the files or
 * {@code topicsText}.
 *
 * @param courseId the course a buddy session studies, since a buddy connection has none; ignored for
 *                 a group session, which always studies its group's course
 */
public record SessionPlanInput(
        Long courseId,
        StudyGoal studyGoal,
        String topicsText) {
}
