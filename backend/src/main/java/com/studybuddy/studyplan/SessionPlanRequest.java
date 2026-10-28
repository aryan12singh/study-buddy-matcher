package com.studybuddy.studyplan;

import com.studybuddy.course.Course;
import com.studybuddy.student.StudyGoal;

/**
 * A session plan request that has passed its checks, carried from the short read before the AI call
 * to the short write after it. The frame is kept so the write can tell whether the session's
 * meetings changed while Gemini answered. Internal to the service layer; never an API response.
 */
record SessionPlanRequest(Long sessionId, Course course, StudyGoal goal, RoadmapFrame frame) {
}
