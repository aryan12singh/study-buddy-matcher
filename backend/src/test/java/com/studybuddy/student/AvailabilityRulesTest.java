package com.studybuddy.student;

import com.studybuddy.common.error.InvalidInputException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AvailabilityRulesTest {
    private static final int MAX_SLOTS = 3;
    private final AvailabilityRules rules = new AvailabilityRules(new AvailabilityProperties(MAX_SLOTS));

    private static AvailabilitySlotDto slot(DayOfWeek day, String start, String end) {
        return new AvailabilitySlotDto(day, LocalTime.parse(start), LocalTime.parse(end));
    }

    @Test void anEmptyWeekIsAllowed() {
        assertDoesNotThrow(() -> rules.validate(List.of()));
    }

    @Test void separateAndTouchingBlocksAreAllowed() {
        assertDoesNotThrow(() -> rules.validate(List.of(
                slot(DayOfWeek.TUESDAY, "09:00", "11:00"),
                slot(DayOfWeek.TUESDAY, "11:00", "12:00"),
                slot(DayOfWeek.THURSDAY, "18:00", "20:00"))));
    }

    @Test void moreBlocksThanTheConfiguredLimitAreRejected() {
        List<AvailabilitySlotDto> atLimit = List.of(slot(DayOfWeek.MONDAY, "09:00", "10:00"),
                slot(DayOfWeek.TUESDAY, "09:00", "10:00"), slot(DayOfWeek.WEDNESDAY, "09:00", "10:00"));
        assertDoesNotThrow(() -> rules.validate(atLimit));
        List<AvailabilitySlotDto> overLimit = new ArrayList<>(atLimit);
        overLimit.add(slot(DayOfWeek.THURSDAY, "09:00", "10:00"));
        InvalidInputException error = assertThrows(InvalidInputException.class, () -> rules.validate(overLimit));
        assertTrue(error.getFieldErrors().containsKey("availability"));
    }

    @Test void aMissingListIsRejected() {
        assertThrows(InvalidInputException.class, () -> rules.validate(null));
    }

    @Test void aBlockMissingADayOrTimeIsRejected() {
        List<AvailabilitySlotDto> withNull = new ArrayList<>(Collections.singletonList(null));
        assertThrows(InvalidInputException.class, () -> rules.validate(withNull));
        assertThrows(InvalidInputException.class, () -> rules.validate(List.of(
                new AvailabilitySlotDto(null, LocalTime.of(9, 0), LocalTime.of(10, 0)))));
    }

    @Test void aBlockMustStartBeforeItEnds() {
        var error = assertThrows(InvalidInputException.class,
                () -> rules.validate(List.of(slot(DayOfWeek.MONDAY, "10:00", "10:00"))));
        assertEquals("Each time block must start before it ends", error.getFieldErrors().get("availability"));
    }

    @Test void secondsAreRejected() {
        assertThrows(InvalidInputException.class,
                () -> rules.validate(List.of(slot(DayOfWeek.MONDAY, "10:00:30", "11:00"))));
    }

    @Test void overlappingBlocksOnTheSameDayAreRejected() {
        var error = assertThrows(InvalidInputException.class, () -> rules.validate(List.of(
                slot(DayOfWeek.WEDNESDAY, "19:00", "21:00"),
                slot(DayOfWeek.WEDNESDAY, "18:00", "19:30"))));
        assertEquals("Time blocks on the same day cannot overlap", error.getFieldErrors().get("availability"));
    }

    @Test void theSameHoursOnDifferentDaysAreAllowed() {
        assertDoesNotThrow(() -> rules.validate(List.of(
                slot(DayOfWeek.MONDAY, "18:00", "20:00"),
                slot(DayOfWeek.FRIDAY, "18:00", "20:00"))));
    }
}
