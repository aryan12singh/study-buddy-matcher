package com.studybuddy.seed;

import com.studybuddy.student.AvailabilitySlotDto;
import com.studybuddy.studygroup.GroupAvailabilitySlotDto;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoDataTest {

    private final DemoData data = new DemoData("presenter", List.of(),
        Map.of("WED_EVE", new DemoData.TimeBlock(DayOfWeek.WEDNESDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
        List.of(), List.of(), List.of(), List.of());

    @Test
    void namedTimeBlocksBecomeStudentAndGroupSlots() {
        assertEquals(List.of(new AvailabilitySlotDto(DayOfWeek.WEDNESDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
            data.studentSlots(List.of("WED_EVE")));
        assertEquals(List.of(new GroupAvailabilitySlotDto(DayOfWeek.WEDNESDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
            data.groupSlots(List.of("WED_EVE")));
    }

    @Test
    void anUnknownTimeBlockFailsWithItsName() {
        var error = assertThrows(IllegalStateException.class, () -> data.studentSlots(List.of("SUN_DAWN")));
        assertTrue(error.getMessage().contains("SUN_DAWN"), error.getMessage());
    }
}
