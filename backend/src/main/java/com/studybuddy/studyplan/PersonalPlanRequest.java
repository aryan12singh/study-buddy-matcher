package com.studybuddy.studyplan;

import com.studybuddy.course.Course;
import com.studybuddy.student.StudyGoal;

import java.time.LocalDate;

/**
 * A personal plan request that has passed its checks, carried from the short read before the AI
 * call to the short write after it. Internal to the service layer; never an API response.
 */
record PersonalPlanRequest(Course course, StudyGoal goal, LocalDate examDate, RoadmapFrame frame) {
}
