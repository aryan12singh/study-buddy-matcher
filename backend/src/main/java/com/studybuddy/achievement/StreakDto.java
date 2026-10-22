package com.studybuddy.achievement;

import java.time.LocalDate;
import java.util.List;

/**
 * Consecutive weeks (Monday start) with at least one scheduled study session the student takes part in.
 * {@code currentRunWeeks} lists the Monday of each week in the current run, oldest first.
 */
public record StreakDto(int currentWeeks, int bestWeeks, List<LocalDate> currentRunWeeks) {
}
