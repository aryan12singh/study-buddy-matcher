package com.studybuddy.studyplan;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadmapFrameTest {
    private static final LocalDate THURSDAY = LocalDate.of(2026, 10, 29);

    @Test
    void weeksAreNumberedFromOneWithTheWeeklyTimeAndNoDate() {
        RoadmapFrame frame = RoadmapFrame.weeks(3, 360);

        assertEquals(RoadmapFrame.BlockKind.WEEK, frame.kind());
        assertEquals(List.of(1, 2, 3), frame.blocks().stream().map(RoadmapBlock::sequence).toList());
        assertTrue(frame.blocks().stream().allMatch(block -> block.availableMinutes() == 360));
        assertNull(frame.blocks().getFirst().date());
    }

    @Test
    void meetingsKeepTheirDatesInOrder() {
        RoadmapFrame frame = RoadmapFrame.meetings(List.of(THURSDAY, THURSDAY.plusWeeks(1)), 120);

        assertEquals(RoadmapFrame.BlockKind.MEETING, frame.kind());
        assertEquals(new RoadmapBlock(2, THURSDAY.plusWeeks(1), 120), frame.blocks().get(1));
    }

    @Test
    void findsABlockByItsNumber() {
        RoadmapFrame frame = RoadmapFrame.weeks(2, 60);

        assertEquals(2, frame.block(2).orElseThrow().sequence());
        assertTrue(frame.block(3).isEmpty());
        assertTrue(frame.block(0).isEmpty());
    }

    @Test
    void refusesAnEmptyFrameOrABlockWithoutTime() {
        assertThrows(IllegalArgumentException.class, () -> RoadmapFrame.weeks(0, 60));
        assertThrows(IllegalArgumentException.class, () -> RoadmapFrame.meetings(List.of(), 60));
        assertThrows(IllegalArgumentException.class, () -> RoadmapFrame.weeks(2, 0));
    }
}
