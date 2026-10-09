package com.studybuddy.student;

import com.studybuddy.common.error.InvalidInputException;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Checks a student's whole weekly availability before it replaces the saved
 * one. Overlapping blocks are rejected so the matching engine's overlap maths
 * never counts the same hour twice. There is no limit on the number of blocks.
 */
@Component
public class AvailabilityRules {
    private static final String FIELD = "availability";

    public void validate(List<AvailabilitySlotDto> slots) {
        if (slots == null) {
            throw new InvalidInputException(FIELD, "Availability is required, even if it is empty");
        }
        slots.forEach(AvailabilityRules::validateSlot);
        rejectOverlaps(slots);
    }

    private static void validateSlot(AvailabilitySlotDto slot) {
        if (slot == null || slot.dayOfWeek() == null || slot.startTime() == null || slot.endTime() == null) {
            throw new InvalidInputException(FIELD, "Each time block needs a day, start time and end time");
        }
        if (!slot.startTime().isBefore(slot.endTime())) {
            throw new InvalidInputException(FIELD, "Each time block must start before it ends");
        }
        if (!isWholeMinute(slot.startTime()) || !isWholeMinute(slot.endTime())) {
            throw new InvalidInputException(FIELD, "Weekly times must use whole minutes");
        }
    }

    private static boolean isWholeMinute(LocalTime time) {
        return time.getSecond() == 0 && time.getNano() == 0;
    }

    /** Blocks that only touch (one ends as the next starts) are allowed. */
    private static void rejectOverlaps(List<AvailabilitySlotDto> slots) {
        List<AvailabilitySlotDto> ordered = slots.stream()
                .sorted(Comparator.comparing(AvailabilitySlotDto::dayOfWeek)
                        .thenComparing(AvailabilitySlotDto::startTime))
                .toList();
        for (int index = 1; index < ordered.size(); index++) {
            AvailabilitySlotDto previous = ordered.get(index - 1);
            AvailabilitySlotDto current = ordered.get(index);
            if (previous.dayOfWeek() == current.dayOfWeek() && current.startTime().isBefore(previous.endTime())) {
                throw new InvalidInputException(FIELD, "Time blocks on the same day cannot overlap");
            }
        }
    }
}
