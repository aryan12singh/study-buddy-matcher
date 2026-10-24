package com.studybuddy.studyplan;

import com.studybuddy.common.error.InvalidInputException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Decides which weeks a personal plan covers. It always starts today in the students' time zone:
 * without an exam it covers this week alone, and with one it covers every week up to the exam,
 * the last possibly a part week, within the configured number of weeks.
 */
@Component
public class StudyPlanCalendar {
    private static final int MINUTES_PER_HOUR = 60;
    private static final int DAYS_PER_WEEK = 7;

    private final StudyPlanProperties properties;
    private final Clock clock;

    public StudyPlanCalendar(StudyPlanProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock.withZone(ZoneId.of(properties.timeZone()));
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** @param examDate the exam to plan up to, or null for a plan of this week alone */
    public RoadmapFrame personalWeeks(LocalDate examDate, int hoursPerWeek) {
        if (hoursPerWeek < 1 || hoursPerWeek > properties.maxHoursPerWeek()) {
            throw new InvalidInputException("hoursPerWeek",
                    "Hours per week must be 1 to " + properties.maxHoursPerWeek());
        }
        LocalDate today = today();
        return RoadmapFrame.weeks(today, weeksUntil(examDate, today), hoursPerWeek * MINUTES_PER_HOUR);
    }

    /** An exam in 1 to 7 days is one week away, in 8 to 14 days two weeks, and so on. */
    private int weeksUntil(LocalDate examDate, LocalDate today) {
        if (examDate == null) {
            return 1;
        }
        long days = ChronoUnit.DAYS.between(today, examDate);
        if (days < 1) {
            throw new InvalidInputException("examDate", "The exam date must be after today");
        }
        long weeks = (days + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK;
        if (weeks > properties.maxWeeks()) {
            throw new InvalidInputException("examDate",
                    "The exam must be within " + properties.maxWeeks() + " weeks; plan nearer the time");
        }
        return (int) weeks;
    }
}
