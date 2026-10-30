package com.studybuddy.seed;

import com.studybuddy.student.AvailabilitySlotDto;
import com.studybuddy.studygroup.GroupAvailabilitySlotDto;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoDataTest {

    private final DemoData data = new DemoData("presenter", List.of(),
        Map.of("WED_EVE", new DemoData.TimeBlock(DayOfWeek.WEDNESDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

    @Test
    void namedTimeBlocksBecomeStudentAndGroupSlots() {
        assertEquals(List.of(new AvailabilitySlotDto(DayOfWeek.WEDNESDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
            data.studentSlots(List.of("WED_EVE")));
        assertEquals(List.of(new GroupAvailabilitySlotDto(DayOfWeek.WEDNESDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
            data.groupSlots(List.of("WED_EVE")));
    }

    @Test
    void aBlockStartsLaterTodayOrOnItsNextDay() {
        DemoData.TimeBlock wednesdayEvening = data.block("WED_EVE");
        LocalDateTime wednesdayNoon = LocalDateTime.of(2026, 10, 28, 12, 0);

        assertEquals(LocalDateTime.of(2026, 10, 28, 19, 0), wednesdayEvening.nextStartAfter(wednesdayNoon));
        assertEquals(LocalDateTime.of(2026, 11, 4, 19, 0), wednesdayEvening.nextStartAfter(wednesdayNoon.withHour(19)));
        assertEquals(LocalDateTime.of(2026, 11, 4, 19, 0), wednesdayEvening.nextStartAfter(wednesdayNoon.plusDays(1)));
    }

    @Test
    void anUnknownTimeBlockFailsWithItsName() {
        var error = assertThrows(IllegalStateException.class, () -> data.studentSlots(List.of("SUN_DAWN")));
        assertTrue(error.getMessage().contains("SUN_DAWN"), error.getMessage());
    }
}
