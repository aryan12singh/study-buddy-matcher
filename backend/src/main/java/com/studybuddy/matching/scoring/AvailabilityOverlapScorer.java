package com.studybuddy.matching.scoring;

import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.matching.availability.WeeklyAvailability;
import org.springframework.stereotype.Component;

/** Scores the share of the searcher's free time that the candidate is also free. */
@Component
public class AvailabilityOverlapScorer implements CriterionScorer {

    @Override
    public MatchingCriterion criterion() {
        return MatchingCriterion.AVAILABILITY;
    }

    @Override
    public double score(MatchingProfile searcher, MatchingProfile candidate, MatchSearchCriteria search) {
        WeeklyAvailability mine = searcher.availability();
        WeeklyAvailability theirs = candidate.availability();
        if (mine.isEmpty() || theirs.isEmpty()) {
            return NEUTRAL_SCORE;
        }
        return Math.min(1.0, (double) mine.sharedMinutesWith(theirs) / mine.totalMinutes());
    }
}
