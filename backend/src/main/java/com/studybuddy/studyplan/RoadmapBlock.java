package com.studybuddy.studyplan;

import java.time.LocalDate;
import java.util.Objects;

/**
 * One block a roadmap is divided into: a week of study, or one meeting of a session, with the most
 * study time its entries may add up to.
 *
 * @param date the week's first day, or the meeting's date
 */
public record RoadmapBlock(int sequence, LocalDate date, int availableMinutes) {
    public RoadmapBlock {
        if (sequence < 1) throw new IllegalArgumentException("Blocks are numbered from 1");
        Objects.requireNonNull(date, "date");
        if (availableMinutes <= 0) throw new IllegalArgumentException("A block needs study time");
    }
}
