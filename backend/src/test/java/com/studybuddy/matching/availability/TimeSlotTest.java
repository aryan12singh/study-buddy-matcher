package com.studybuddy.matching.availability;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.studybuddy.matching.MatchingFixtures.slot;
import static com.studybuddy.matching.MatchingFixtures.week;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.TUESDAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeSlotTest {

    @Test
    void durationIsInMinutes() {
        assertEquals(90, slot(MONDAY, "09:00", "10:30").durationMinutes());
    }

    @Test
    void partialOverlapCountsOnlyTheSharedPart() {
        assertEquals(60, slot(MONDAY, "09:00", "11:00").overlapMinutes(slot(MONDAY, "10:00", "12:00")));
    }

    @Test
    void overlapIsTheSameFromEitherSide() {
        TimeSlot morning = slot(MONDAY, "09:00", "11:00");
        TimeSlot late = slot(MONDAY, "10:15", "13:00");

        assertEquals(morning.overlapMinutes(late), late.overlapMinutes(morning));
    }

    @Test
    void slotInsideAnotherOverlapsForItsWholeLength() {
        assertEquals(30, slot(MONDAY, "09:00", "12:00").overlapMinutes(slot(MONDAY, "10:00", "10:30")));
    }

    @Test
    void slotsThatOnlyTouchDoNotOverlap() {
        assertEquals(0, slot(MONDAY, "09:00", "10:00").overlapMinutes(slot(MONDAY, "10:00", "11:00")));
    }

    @Test
    void slotsOnDifferentDaysDoNotOverlap() {
        assertEquals(0, slot(MONDAY, "09:00", "11:00").overlapMinutes(slot(TUESDAY, "09:00", "11:00")));
    }

    @Test
    void intersectionIsTheSharedBlock() {
        assertEquals(Optional.of(slot(MONDAY, "10:00", "11:00")),
                slot(MONDAY, "09:00", "11:00").intersection(slot(MONDAY, "10:00", "12:00")));
    }

    @Test
    void slotsThatOnlyTouchOrAreOnDifferentDaysHaveNoIntersection() {
        assertTrue(slot(MONDAY, "09:00", "10:00").intersection(slot(MONDAY, "10:00", "11:00")).isEmpty());
        assertTrue(slot(MONDAY, "09:00", "11:00").intersection(slot(TUESDAY, "09:00", "11:00")).isEmpty());
    }

    @Test
    void rejectsASlotThatDoesNotStartBeforeItEnds() {
        assertThrows(IllegalArgumentException.class, () -> slot(MONDAY, "10:00", "10:00"));
        assertThrows(IllegalArgumentException.class, () -> slot(MONDAY, "11:00", "10:00"));
        assertThrows(IllegalArgumentException.class, () -> new TimeSlot(null, LocalTime.NOON, LocalTime.MIDNIGHT));
    }

    @Test
    void weeklySharedTimeAddsUpEveryOverlappingPair() {
        WeeklyAvailability mine = week(slot(MONDAY, "09:00", "11:00"), slot(DayOfWeek.WEDNESDAY, "14:00", "16:00"));
        WeeklyAvailability theirs = week(slot(MONDAY, "10:00", "12:00"), slot(DayOfWeek.WEDNESDAY, "13:00", "14:30"),
                slot(DayOfWeek.FRIDAY, "09:00", "17:00"));

        assertEquals(90, mine.sharedMinutesWith(theirs));
        assertEquals(240, mine.totalMinutes());
    }

    @Test
    void sharedSlotsAreListedInWeekOrder() {
        WeeklyAvailability mine = week(slot(DayOfWeek.WEDNESDAY, "14:00", "16:00"), slot(MONDAY, "09:00", "11:00"));
        WeeklyAvailability theirs = week(slot(MONDAY, "10:00", "12:00"), slot(DayOfWeek.WEDNESDAY, "13:00", "14:30"),
                slot(DayOfWeek.FRIDAY, "09:00", "17:00"));

        assertEquals(List.of(slot(MONDAY, "10:00", "11:00"), slot(DayOfWeek.WEDNESDAY, "14:00", "14:30")),
                mine.sharedSlotsWith(theirs));
    }

    @Test
    void duplicateOverlappingAndAdjacentInputCountsEveryMinuteOnce() {
        WeeklyAvailability mine = week(slot(MONDAY, "09:00", "11:00"), slot(MONDAY, "09:00", "11:00"),
                slot(MONDAY, "10:00", "12:00"), slot(MONDAY, "12:00", "13:00"));
        WeeklyAvailability theirs = week(slot(MONDAY, "10:00", "12:00"), slot(MONDAY, "11:00", "14:00"),
                slot(MONDAY, "10:00", "12:00"), slot(TUESDAY, "09:00", "11:00"));
        assertEquals(240, mine.totalMinutes());
        assertEquals(360, theirs.totalMinutes());
        assertEquals(180, mine.sharedMinutesWith(theirs));
        assertEquals(180, theirs.sharedMinutesWith(mine));
        assertEquals(List.of(slot(MONDAY, "10:00", "13:00")), mine.sharedSlotsWith(theirs));
        assertEquals(List.of(slot(MONDAY, "09:00", "13:00")), mine.slots());
    }

    @Test
    void sessionsIgnoreSharedBlocksTooShortToMeetIn() {
        WeeklyAvailability mine = week(slot(MONDAY, "09:00", "11:00"), slot(TUESDAY, "09:00", "11:00"));
        WeeklyAvailability theirs = week(slot(MONDAY, "10:45", "12:00"), slot(TUESDAY, "08:00", "10:00"));

        assertEquals(List.of(slot(TUESDAY, "09:00", "10:00")), mine.sharedSessionsWith(theirs, 60));
        assertEquals(60, mine.sessionMinutesWith(theirs, 60));
        assertEquals(75, mine.sessionMinutesWith(theirs, 0), "a zero minimum keeps every overlap");
        assertEquals(75, mine.sharedMinutesWith(theirs), "the raw interval maths is unchanged");
    }

    @Test
    void emptyWeekSharesNothing() {
        WeeklyAvailability empty = week();

        assertTrue(empty.isEmpty());
        assertEquals(0, empty.sharedMinutesWith(week(slot(MONDAY, "09:00", "10:00"))));
        assertTrue(empty.sharedSlotsWith(week(slot(MONDAY, "09:00", "10:00"))).isEmpty());
    }
}
