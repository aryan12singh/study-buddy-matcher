package com.studybuddy.matching.strategy;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

/** Ranks by timetable overlap first, breaking ties with the weighted total of the other criteria. */
@Component
public class AvailabilityFirstStrategy implements MatchingStrategy {

    @Override
    public MatchingStrategyType type() {
        return MatchingStrategyType.AVAILABILITY_FIRST;
    }

    @Override
    public Comparator<MatchScore> ranking() {
        return Comparator.comparingDouble((MatchScore score) -> score.scoreFor(MatchingCriterion.AVAILABILITY))
                .thenComparingDouble(MatchScore::total)
                .reversed();
    }

    /** Availability already decides the order, so it is not weighted again in the total. */
    @Override
    public Set<MatchingCriterion> weightedCriteria() {
        return EnumSet.complementOf(EnumSet.of(MatchingCriterion.AVAILABILITY));
    }
}
