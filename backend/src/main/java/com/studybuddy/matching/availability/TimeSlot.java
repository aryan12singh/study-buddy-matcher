package com.studybuddy.matching.availability;

import com.studybuddy.student.AvailabilitySlot;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;

/** One weekly block of free time. Immutable; always starts before it ends on the same day. */
public record TimeSlot(DayOfWeek day, LocalTime start, LocalTime end) {

    public TimeSlot {
        if (day == null || start == null || end == null) {
            throw new IllegalArgumentException("A time slot needs a day, start and end");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("A time slot must start before it ends");
        }
    }

    public static TimeSlot from(AvailabilitySlot slot) {
        return new TimeSlot(slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime());
    }

    public long durationMinutes() {
        return Duration.between(start, end).toMinutes();
    }

    /** Minutes both slots are free at the same time; zero on different days or when they only touch. */
    public long overlapMinutes(TimeSlot other) {
        if (day != other.day) {
            return 0;
        }
        LocalTime laterStart = start.isAfter(other.start) ? start : other.start;
        LocalTime earlierEnd = end.isBefore(other.end) ? end : other.end;
        return laterStart.isBefore(earlierEnd) ? Duration.between(laterStart, earlierEnd).toMinutes() : 0;
    }
}
