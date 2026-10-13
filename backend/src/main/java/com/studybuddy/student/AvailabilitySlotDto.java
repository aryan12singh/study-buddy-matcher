package com.studybuddy.student;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * One weekly block of time, used both when saving availability and when showing it.
 * {@link AvailabilityRules} checks it, so the save path has a single set of rules.
 */
public record AvailabilitySlotDto(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
}
