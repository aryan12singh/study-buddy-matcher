package com.studybuddy.studygroup;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** One weekly block a group meets, used both when saving a group and when showing it. */
public record GroupAvailabilitySlotDto(
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime) {
}
