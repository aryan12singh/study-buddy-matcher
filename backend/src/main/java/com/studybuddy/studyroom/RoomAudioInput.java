package com.studybuddy.studyroom;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RoomAudioInput(@NotNull AudioPreset preset, @NotNull Boolean playing, @NotNull @PositiveOrZero Long expectedVersion) {}
