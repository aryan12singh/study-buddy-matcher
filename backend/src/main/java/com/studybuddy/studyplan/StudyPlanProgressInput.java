package com.studybuddy.studyplan;

import jakarta.validation.constraints.NotNull;

/** Ticks a plan entry off, or un-ticks it. */
public record StudyPlanProgressInput(@NotNull Boolean completed) {
}
