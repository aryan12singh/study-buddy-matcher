package com.studybuddy.matching.scoring;

import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.matching.availability.WeeklyAvailability;
import org.springframework.stereotype.Component;

/**
 * Scores shared free time as a share of the smaller of the two timetables. Dividing by
 * the smaller one makes the score the same from either student's side, and does not
 * penalise a student for honestly listing a lot of free time.
 */
@Component
public class AvailabilityOverlapScorer implements CriterionScorer {

    private final ScoringRules rules;

    public AvailabilityOverlapScorer(ScoringRules rules) {
        this.rules = rules;
    }

    @Override
    public MatchingCriterion criterion() {
        return MatchingCriterion.AVAILABILITY;
    }

    @Override
    public CriterionResult score(MatchingProfile searcher, MatchingProfile candidate, MatchSearchCriteria search) {
        WeeklyAvailability mine = searcher.availability();
        WeeklyAvailability theirs = candidate.availability();
        if (mine.isEmpty() || theirs.isEmpty()) {
            return CriterionResult.unknown();
        }
        long smallerTimetable = Math.min(mine.totalMinutes(), theirs.totalMinutes());
        return CriterionResult.of(Math.min(1.0, (double) mine.sessionMinutesWith(theirs, rules.minSessionMinutes()) / smallerTimetable));
    }
}
