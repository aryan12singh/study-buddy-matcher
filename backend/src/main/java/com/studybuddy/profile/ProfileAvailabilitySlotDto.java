package com.studybuddy.profile;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ProfileAvailabilitySlotDto(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
}
