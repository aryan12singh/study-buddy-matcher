package com.studybuddy.matchrequest;

import com.studybuddy.student.StudyGoal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Where a match request was sent from, and the course and study goal it was about, if any. A
 * request sent without one is treated as sent from a profile.
 */
public record MatchRequestContext(@NotNull MatchRequestOrigin origin, @Positive Long courseId, StudyGoal studyGoal) {

    public static MatchRequestContext profile() {
        return new MatchRequestContext(MatchRequestOrigin.PROFILE, null, null);
    }
}
