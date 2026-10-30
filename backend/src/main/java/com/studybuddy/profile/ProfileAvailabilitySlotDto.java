package com.studybuddy.profile;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** One weekly availability slot shown on a student's profile. */
public record ProfileAvailabilitySlotDto(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
}
