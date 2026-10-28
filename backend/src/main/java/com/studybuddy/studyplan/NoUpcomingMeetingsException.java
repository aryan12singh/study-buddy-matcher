package com.studybuddy.studyplan;

import com.studybuddy.common.error.ApiException;
import org.springframework.http.HttpStatus;

/** A session plan for a session that is cancelled or whose meetings have all ended: nothing to plan. */
public class NoUpcomingMeetingsException extends ApiException {
    public NoUpcomingMeetingsException(Long sessionId) {
        super(HttpStatus.CONFLICT, "NO_UPCOMING_MEETINGS",
                "Study session " + sessionId + " has no upcoming meetings to plan");
    }
}
