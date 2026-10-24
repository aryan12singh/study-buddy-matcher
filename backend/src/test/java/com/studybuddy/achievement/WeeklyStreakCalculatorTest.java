package com.studybuddy.achievement;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeeklyStreakCalculatorTest {
    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22); // a Thursday
    private final WeeklyStreakCalculator calculator = new WeeklyStreakCalculator();

    private static Instant on(String date) {
        return LocalDate.parse(date).atTime(10, 0).atZone(SINGAPORE).toInstant();
    }

    @Test
    void noSessionsMeansNoStreak() {
        StreakDto streak = calculator.calculate(List.of(), SINGAPORE, TODAY);
        assertThat(streak.currentWeeks()).isZero();
        assertThat(streak.bestWeeks()).isZero();
        assertThat(streak.currentRunWeeks()).isEmpty();
    }

    @Test
    void countsConsecutiveWeeksEndingThisWeekAndMergesSessionsInOneWeek() {
        StreakDto streak = calculator.calculate(List.of(on("2026-10-06"), on("2026-10-13"), on("2026-10-15"), on("2026-10-20")), SINGAPORE, TODAY);
        assertThat(streak.currentWeeks()).isEqualTo(3);
        assertThat(streak.currentRunWeeks()).containsExactly(
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 19));
    }

    @Test
    void streakEndingLastWeekIsStillCurrent() {
        StreakDto streak = calculator.calculate(List.of(on("2026-10-08"), on("2026-10-14")), SINGAPORE, TODAY);
        assertThat(streak.currentWeeks()).isEqualTo(2);
    }

    @Test
    void gapBreaksTheCurrentStreakButBestIsKept() {
        StreakDto streak = calculator.calculate(List.of(on("2026-09-01"), on("2026-09-08"), on("2026-09-15"), on("2026-10-21")), SINGAPORE, TODAY);
        assertThat(streak.currentWeeks()).isEqualTo(1);
        assertThat(streak.bestWeeks()).isEqualTo(3);
    }

    @Test
    void futureWeeksAreNotCounted() {
        StreakDto streak = calculator.calculate(List.of(on("2026-10-27"), on("2026-11-03")), SINGAPORE, TODAY);
        assertThat(streak.currentWeeks()).isZero();
        assertThat(streak.bestWeeks()).isZero();
    }

    @Test
    void weekBoundaryUsesConfiguredZone() {
        // Sunday in UTC has become Monday in Singapore; the configured zone wins.
        Instant boundary = Instant.parse("2026-10-18T16:30:00Z");
        assertThat(calculator.calculate(List.of(boundary), SINGAPORE, TODAY).currentRunWeeks())
                .containsExactly(LocalDate.of(2026, 10, 19));
        assertThat(calculator.calculate(List.of(boundary), ZoneId.of("UTC"), TODAY).currentRunWeeks())
                .containsExactly(LocalDate.of(2026, 10, 12));
    }

    @Test
    void plannedOccurrenceLaterThisWeekCountsBeforeItsStartButNextWeekDoesNot() {
        StreakDto streak = calculator.calculate(List.of(on("2026-10-25"), on("2026-10-26")), SINGAPORE, TODAY);
        assertThat(streak.currentWeeks()).isEqualTo(1);
        assertThat(streak.bestWeeks()).isEqualTo(1);
        assertThat(streak.currentRunWeeks()).containsExactly(LocalDate.of(2026, 10, 19));
    }

    @Test
    void aRunEndingTwoWeeksAgoIsNoLongerCurrent() {
        StreakDto streak = calculator.calculate(List.of(on("2026-10-06")), SINGAPORE, TODAY);
        assertThat(streak.currentWeeks()).isZero();
        assertThat(streak.bestWeeks()).isEqualTo(1);
    }
}
