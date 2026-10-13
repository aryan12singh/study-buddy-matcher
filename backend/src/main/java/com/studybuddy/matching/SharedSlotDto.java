package com.studybuddy.matching;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** One weekly block when the searcher and a match are both free. */
public record SharedSlotDto(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
}
