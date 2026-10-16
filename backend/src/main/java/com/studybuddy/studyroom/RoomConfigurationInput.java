package com.studybuddy.studyroom;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record RoomConfigurationInput(@NotNull @Min(1) Integer focusMinutes,
    @NotNull @Min(1) Integer breakMinutes, @NotNull @Min(1) Integer participantLimit,
    @Positive Long hostId, @Positive Long coHostId, @NotNull @PositiveOrZero Long expectedVersion) {}
