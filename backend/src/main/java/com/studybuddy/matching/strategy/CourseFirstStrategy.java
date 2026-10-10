package com.studybuddy.matching.strategy;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

/** Ranks by course match first, then by the weighted total of the remaining preferences. */
@Component
public class CourseFirstStrategy implements MatchingStrategy {

    @Override
    public MatchingStrategyType type() {
        return MatchingStrategyType.COURSE_FIRST;
    }

    @Override
    public Comparator<MatchScore> ranking() {
        return Comparator.comparingDouble((MatchScore score) -> score.scoreFor(MatchingCriterion.COURSE))
                .thenComparingDouble(MatchScore::total)
                .reversed();
    }

    /** Course already decides the order, so it is not weighted again in the total. */
    @Override
    public Set<MatchingCriterion> weightedCriteria() {
        return EnumSet.complementOf(EnumSet.of(MatchingCriterion.COURSE));
    }
}
