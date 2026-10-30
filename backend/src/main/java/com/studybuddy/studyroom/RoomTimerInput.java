package com.studybuddy.studyroom;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * A timer command from the leader or a present host. {@code expectedVersion} is the room version
 * they last saw, so two people pressing at once cannot both apply.
 */
public record RoomTimerInput(@NotNull TimerCommand command, @NotNull @PositiveOrZero Long expectedVersion) {}
