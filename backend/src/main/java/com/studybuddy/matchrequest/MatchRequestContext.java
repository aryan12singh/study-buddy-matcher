package com.studybuddy.matchrequest;

import com.studybuddy.student.StudyGoal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MatchRequestContext(@NotNull MatchRequestOrigin origin, @Positive Long courseId, StudyGoal studyGoal) {

    public static MatchRequestContext profile() {
        return new MatchRequestContext(MatchRequestOrigin.PROFILE, null, null);
    }
}
