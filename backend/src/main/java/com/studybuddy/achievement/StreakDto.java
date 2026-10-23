package com.studybuddy.achievement;

import java.time.LocalDate;
import java.util.List;

/**
 * Consecutive weeks (Monday start) with at least one eligible planned occurrence.
 * Both runs are derived from current records, without recording attendance or permanent history.
 * {@code currentRunWeeks} lists the Monday of each week in the current run, oldest first.
 */
public record StreakDto(int currentWeeks, int bestWeeks, List<LocalDate> currentRunWeeks, String weekZone) {
}
