package com.studybuddy.studyroom;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RoomAudioInput(AudioPreset musicPreset, @NotNull Boolean musicPlaying,
    AudioPreset ambientPreset, @NotNull Boolean ambientPlaying, @NotNull @PositiveOrZero Long expectedVersion) {}
