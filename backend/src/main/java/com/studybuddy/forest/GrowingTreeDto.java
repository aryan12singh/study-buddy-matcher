package com.studybuddy.forest;

/**
 * The tree a student is growing in one room right now. {@code species} is null until the visit is
 * long enough to plant anything, and {@code next} is null once the grandest tree is reached.
 */
public record GrowingTreeDto(boolean visiting, long focusedMinutes, TreeSpecies species, TreeSpecies next,
    Integer minutesToNext, TreeLight light) {}
