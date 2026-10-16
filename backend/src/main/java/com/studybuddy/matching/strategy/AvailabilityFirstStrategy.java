package com.studybuddy.matching.strategy;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import org.springframework.stereotype.Component;

/**
 * Ranks by bands of shared hours per week first (by default 3 h or more, 1 to 3 h,
 * under 1 h), then by the weighted total. A student with no timetable shares no
 * confirmed hours, so lands in the lowest band.
 */
@Component
public class AvailabilityFirstStrategy extends PriorityTierStrategy {

    public AvailabilityFirstStrategy(PriorityTiers tiers) {
        super(tiers.availabilityHours());
    }

    @Override
    public MatchingStrategyType type() {
        return MatchingStrategyType.AVAILABILITY_FIRST;
    }

    @Override
    protected MatchingCriterion priority() {
        return MatchingCriterion.AVAILABILITY;
    }

    @Override
    protected double priorityValue(MatchScore score) {
        return score.sharedHoursPerWeek();
    }
}
