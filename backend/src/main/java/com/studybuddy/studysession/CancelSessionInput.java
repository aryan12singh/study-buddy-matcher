package com.studybuddy.studysession;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Cancels a study session. {@code expectedVersion} is the version the student last saw, so a
 * cancellation over a newer edit is refused.
 */
public record CancelSessionInput(@NotNull @PositiveOrZero Long expectedVersion) { }
