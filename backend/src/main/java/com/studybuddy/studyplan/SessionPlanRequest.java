package com.studybuddy.studyplan;

import com.studybuddy.course.Course;
import com.studybuddy.student.StudyGoal;

/**
 * A session plan request that has passed its checks, carried from the short read before the AI call
 * to the short write after it. The session's version is kept so the write can tell whether the
 * session was edited while Gemini answered, and the frame so it can leave out meetings that ended
 * in the meantime. Internal to the service layer; never an API response.
 */
record SessionPlanRequest(Long sessionId, long sessionVersion, Course course, StudyGoal goal, RoadmapFrame frame) {
}
