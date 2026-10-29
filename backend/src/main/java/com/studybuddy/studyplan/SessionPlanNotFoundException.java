package com.studybuddy.studyplan;

import com.studybuddy.common.error.NotFoundException;

/** A session that has no roadmap yet, or one the student does not take part in, so its ID reveals nothing. */
public class SessionPlanNotFoundException extends NotFoundException {
    public SessionPlanNotFoundException(Long sessionId) {
        super("Study session " + sessionId + " has no study plan");
    }
}
