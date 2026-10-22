package com.studybuddy.achievement;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Pure streak maths: turns session start times into runs of consecutive weeks.
 * Weeks after the current one are ignored, so a streak never counts the future.
 * A run that ended last week is still current, because this week may not have a session yet.
 */
public final class WeeklyStreakCalculator {

    public StreakDto calculate(Collection<Instant> sessionStarts, ZoneId zone, LocalDate today) {
        LocalDate thisWeek = weekStart(today);
        TreeSet<LocalDate> weeks = new TreeSet<>();
        for (Instant start : sessionStarts) {
            LocalDate week = weekStart(start.atZone(zone).toLocalDate());
            if (!week.isAfter(thisWeek)) {
                weeks.add(week);
            }
        }
        return new StreakDto(currentRun(weeks, thisWeek).size(), bestRun(weeks), currentRun(weeks, thisWeek));
    }

    private static List<LocalDate> currentRun(TreeSet<LocalDate> weeks, LocalDate thisWeek) {
        LocalDate cursor = weeks.contains(thisWeek) ? thisWeek : thisWeek.minusWeeks(1);
        List<LocalDate> run = new ArrayList<>();
        while (weeks.contains(cursor)) {
            run.addFirst(cursor);
            cursor = cursor.minusWeeks(1);
        }
        return List.copyOf(run);
    }

    private static int bestRun(TreeSet<LocalDate> weeks) {
        int best = 0;
        int run = 0;
        LocalDate previous = null;
        for (LocalDate week : weeks) {
            run = previous != null && previous.plusWeeks(1).equals(week) ? run + 1 : 1;
            best = Math.max(best, run);
            previous = week;
        }
        return best;
    }

    private static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
