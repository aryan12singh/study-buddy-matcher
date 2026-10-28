package com.studybuddy.studyplan;

import com.studybuddy.common.error.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudyPlanCalendarTest {
    /** 23:30 UTC on Thursday is already Friday 23 October in Singapore. */
    private static final Clock LATE_THURSDAY_UTC = Clock.fixed(Instant.parse("2026-10-22T23:30:00Z"), ZoneOffset.UTC);
    private static final LocalDate FRIDAY = LocalDate.of(2026, 10, 23);
    /** At most 4 weeks and 10 hours a week. */
    private static final StudyPlanCalendar CALENDAR = new StudyPlanCalendar(
            new StudyPlanProperties(DataSize.ofMegabytes(10), 6, 4000, 100000, 40, 5, 5, 4, 10, 12, "Asia/Singapore"),
            LATE_THURSDAY_UTC);

    @Test
    void todayFollowsTheStudentsTimeZoneNotTheServers() {
        assertEquals(FRIDAY, CALENDAR.today());
    }

    @Test
    void withoutAnExamThePlanCoversThisWeekFromToday() {
        RoadmapFrame frame = CALENDAR.personalWeeks(null, 6);

        assertEquals(RoadmapFrame.BlockKind.WEEK, frame.kind());
        assertEquals(1, frame.blocks().size());
        assertEquals(FRIDAY, frame.firstDate());
        assertEquals(360, frame.minutesPerBlock());
    }

    @Test
    void anExamOneToSevenDaysAwayIsOneWeekAndEightDaysIsTwo() {
        assertEquals(1, CALENDAR.personalWeeks(FRIDAY.plusDays(1), 6).blocks().size());
        assertEquals(1, CALENDAR.personalWeeks(FRIDAY.plusDays(7), 6).blocks().size());
        assertEquals(2, CALENDAR.personalWeeks(FRIDAY.plusDays(8), 6).blocks().size());
    }

    @Test
    void anExamAtTheWeekLimitIsAllowedAndOneDayMoreIsNot() {
        assertEquals(4, CALENDAR.personalWeeks(FRIDAY.plusDays(28), 6).blocks().size());

        InvalidInputException error = assertThrows(InvalidInputException.class,
                () -> CALENDAR.personalWeeks(FRIDAY.plusDays(29), 6));
        assertTrue(error.getFieldErrors().containsKey("examDate"));
    }

    @Test
    void anExamTodayOrInThePastIsRefused() {
        assertThrows(InvalidInputException.class, () -> CALENDAR.personalWeeks(FRIDAY, 6));
        assertThrows(InvalidInputException.class, () -> CALENDAR.personalWeeks(FRIDAY.minusDays(1), 6));
    }

    @Test
    void weeklyHoursMustBeWithinTheLimit() {
        assertEquals(600, CALENDAR.personalWeeks(null, 10).minutesPerBlock());

        InvalidInputException tooMany = assertThrows(InvalidInputException.class, () -> CALENDAR.personalWeeks(null, 11));
        assertTrue(tooMany.getFieldErrors().containsKey("hoursPerWeek"));
        assertThrows(InvalidInputException.class, () -> CALENDAR.personalWeeks(null, 0));
    }
}
