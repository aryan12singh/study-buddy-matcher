package com.studybuddy.matching.strategy;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Set;

/** Ranks by timetable overlap first, breaking ties with the configured weighted total. */
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

    @Override
    public Comparator<MatchScore> ranking(Set<MatchingCriterion> enabled) {
        return enabled.contains(MatchingCriterion.AVAILABILITY) ? ranking()
                : Comparator.comparingDouble(MatchScore::total).reversed();
    }
}
