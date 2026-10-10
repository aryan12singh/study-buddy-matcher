package com.studybuddy.matching.availability;

import com.studybuddy.student.AvailabilitySlot;

import java.util.List;

/**
 * A student's free time across the week. A student's own slots never overlap
 * (the profile rejects that), so summing pairwise overlaps never counts the same
 * minute twice.
 */
public record WeeklyAvailability(List<TimeSlot> slots) {

    public WeeklyAvailability {
        slots = List.copyOf(slots);
    }

    public static WeeklyAvailability from(List<AvailabilitySlot> slots) {
        return new WeeklyAvailability(slots.stream().map(TimeSlot::from).toList());
    }

    public boolean isEmpty() {
        return slots.isEmpty();
    }

    public long totalMinutes() {
        return slots.stream().mapToLong(TimeSlot::durationMinutes).sum();
    }

    public long sharedMinutesWith(WeeklyAvailability other) {
        return slots.stream()
                .mapToLong(mine -> other.slots.stream().mapToLong(mine::overlapMinutes).sum())
                .sum();
    }
}
