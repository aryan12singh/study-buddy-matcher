package com.studybuddy.studygroup;

import com.studybuddy.student.Student;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static com.studybuddy.studygroup.StudyGroupFixtures.course;
import static com.studybuddy.studygroup.StudyGroupFixtures.group;
import static com.studybuddy.studygroup.StudyGroupFixtures.membership;
import static com.studybuddy.studygroup.StudyGroupFixtures.student;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudyGroupAssemblerTest {

    private final StudyGroupAssembler assembler = new StudyGroupAssembler();

    @Test
    void availabilityIsListedMondayFirstNotAlphabetically() {
        StudyGroup group = group(5L, course(1L, "IS442"), student(1L, "Alice"), 4);
        List<GroupAvailabilitySlot> slots = List.of(
                new GroupAvailabilitySlot(group, DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)),
                new GroupAvailabilitySlot(group, DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(20, 0)),
                new GroupAvailabilitySlot(group, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)));

        StudyGroupDetailDto dto = assembler.toDetail(group, List.of(), slots, null, null);

        assertEquals(List.of(
                        new GroupAvailabilitySlotDto(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                        new GroupAvailabilitySlotDto(DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(20, 0)),
                        new GroupAvailabilitySlotDto(DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(11, 0))),
                dto.availability());
    }

    @Test
    void onlyTheLeaderIsFlaggedAsLeader() {
        Student alice = student(1L, "Alice");
        StudyGroup group = group(5L, course(1L, "IS442"), alice, 4);

        StudyGroupDetailDto dto = assembler.toDetail(group,
                List.of(membership(group, alice), membership(group, student(2L, "Bob"))), List.of(), null, null);

        assertTrue(dto.members().get(0).leader());
        assertFalse(dto.members().get(1).leader());
        assertEquals(2, dto.memberCount());
    }
}
