package com.studybuddy.studyroom;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RoomTimerInput(@NotNull TimerCommand command, @NotNull @PositiveOrZero Long expectedVersion) {}
