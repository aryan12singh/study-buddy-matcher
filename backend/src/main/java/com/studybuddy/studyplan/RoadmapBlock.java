package com.studybuddy.studyplan;

import java.time.LocalDate;

/**
 * One block a roadmap is divided into: a week before the exam, or one meeting of a session, with
 * the most study time its entries may add up to.
 *
 * @param date the meeting's date; null for a personal plan's week
 */
public record RoadmapBlock(int sequence, LocalDate date, int availableMinutes) {
    public RoadmapBlock {
        if (sequence < 1) throw new IllegalArgumentException("Blocks are numbered from 1");
        if (availableMinutes <= 0) throw new IllegalArgumentException("A block needs study time");
    }
}
