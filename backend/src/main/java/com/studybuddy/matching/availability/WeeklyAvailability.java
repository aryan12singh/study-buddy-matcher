package com.studybuddy.matching.availability;

import com.studybuddy.student.AvailabilitySlot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A student's unique free time across the week. Normalizing even legacy or duplicate
 * input keeps every duration and intersection free from double-counting.
 */
public record WeeklyAvailability(List<TimeSlot> slots) {

    public WeeklyAvailability {
        slots = merged(slots);
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
        return sharedSlotsWith(other).stream().mapToLong(TimeSlot::durationMinutes).sum();
    }

    /** The blocks of time both students are free, in week order from Monday morning. */
    public List<TimeSlot> sharedSlotsWith(WeeklyAvailability other) {
        return merged(slots.stream()
                .flatMap(mine -> other.slots.stream().flatMap(theirs -> mine.intersection(theirs).stream()))
                .toList());
    }

    /**
     * The shared blocks long enough to hold a study session, in week order. Shorter
     * fragments, such as a 15-minute overlap, cannot be used to meet, so do not count.
     */
    public List<TimeSlot> sharedSessionsWith(WeeklyAvailability other, int minimumSessionMinutes) {
        return sharedSlotsWith(other).stream()
                .filter(slot -> slot.durationMinutes() >= minimumSessionMinutes)
                .toList();
    }

    public long sessionMinutesWith(WeeklyAvailability other, int minimumSessionMinutes) {
        return sharedSessionsWith(other, minimumSessionMinutes).stream().mapToLong(TimeSlot::durationMinutes).sum();
    }
    private static List<TimeSlot> merged(List<TimeSlot> input) {
        List<TimeSlot> ordered = input.stream()
                .sorted(Comparator.comparing(TimeSlot::day).thenComparing(TimeSlot::start).thenComparing(TimeSlot::end))
                .toList();
        List<TimeSlot> unique = new ArrayList<>();
        for (TimeSlot slot : ordered) {
            if (unique.isEmpty()) {
                unique.add(slot);
                continue;
            }
            TimeSlot previous = unique.getLast();
            if (previous.day() == slot.day() && !slot.start().isAfter(previous.end())) {
                unique.set(unique.size() - 1, new TimeSlot(previous.day(), previous.start(),
                        previous.end().isAfter(slot.end()) ? previous.end() : slot.end()));
            } else {
                unique.add(slot);
            }
        }
        return List.copyOf(unique);
    }
}
