package com.studybuddy.studyplan;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadmapFrameTest {
    private static final LocalDate FRIDAY = LocalDate.of(2026, 10, 23);
    private static final LocalDate THURSDAY = LocalDate.of(2026, 10, 29);

    @Test
    void weeksAreNumberedFromOneAndStartSevenDaysApart() {
        RoadmapFrame frame = RoadmapFrame.weeks(FRIDAY, 3, 360);

        assertEquals(RoadmapFrame.BlockKind.WEEK, frame.kind());
        assertEquals(List.of(1, 2, 3), frame.blocks().stream().map(RoadmapBlock::sequence).toList());
        assertEquals(List.of(FRIDAY, FRIDAY.plusDays(7), FRIDAY.plusDays(14)),
                frame.blocks().stream().map(RoadmapBlock::date).toList());
        assertEquals(FRIDAY, frame.firstDate());
        assertEquals(360, frame.minutesPerBlock());
    }

    @Test
    void aSingleWeekIsAPlanForThatWeek() {
        assertEquals(List.of(new RoadmapBlock(1, FRIDAY, 240)), RoadmapFrame.weeks(FRIDAY, 1, 240).blocks());
    }

    @Test
    void meetingsKeepTheirDatesInOrder() {
        RoadmapFrame frame = RoadmapFrame.meetings(List.of(THURSDAY, THURSDAY.plusWeeks(1)), 120);

        assertEquals(RoadmapFrame.BlockKind.MEETING, frame.kind());
        assertEquals(new RoadmapBlock(2, THURSDAY.plusWeeks(1), 120), frame.blocks().get(1));
        assertEquals(120, frame.minutesPerBlock());
    }

    @Test
    void findsABlockByItsNumber() {
        RoadmapFrame frame = RoadmapFrame.weeks(FRIDAY, 2, 60);

        assertEquals(2, frame.block(2).orElseThrow().sequence());
        assertTrue(frame.block(3).isEmpty());
        assertTrue(frame.block(0).isEmpty());
    }

    @Test
    void refusesAnEmptyFrameOrABlockWithoutTimeOrDate() {
        assertThrows(IllegalArgumentException.class, () -> RoadmapFrame.weeks(FRIDAY, 0, 60));
        assertThrows(IllegalArgumentException.class, () -> RoadmapFrame.meetings(List.of(), 60));
        assertThrows(IllegalArgumentException.class, () -> RoadmapFrame.weeks(FRIDAY, 2, 0));
        assertThrows(NullPointerException.class, () -> new RoadmapBlock(1, null, 60));
    }
}
