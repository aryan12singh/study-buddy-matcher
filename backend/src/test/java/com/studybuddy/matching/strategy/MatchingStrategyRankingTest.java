package com.studybuddy.matching.strategy;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MatchingStrategyRankingTest {

    private static final MatchScore STRONG_COURSE = score(1.0, 0.0, 0.5);
    private static final MatchScore STRONG_AVAILABILITY = score(0.0, 1.0, 0.6);
    private static final MatchScore BEST_OVERALL = score(0.8, 0.8, 0.8);
    private static final MatchScore WEAK = score(0.0, 0.0, 0.1);

    private static final List<MatchScore> UNSORTED = List.of(WEAK, STRONG_COURSE, BEST_OVERALL, STRONG_AVAILABILITY);

    private static MatchScore score(double course, double availability, double total) {
        return new MatchScore(Map.of(MatchingCriterion.COURSE, course, MatchingCriterion.AVAILABILITY, availability), total);
    }

    private static List<MatchScore> rank(MatchingStrategy strategy, List<MatchScore> scores) {
        return scores.stream().sorted(strategy.ranking()).toList();
    }

    @Test
    void balancedRanksByTotal() {
        assertEquals(List.of(BEST_OVERALL, STRONG_AVAILABILITY, STRONG_COURSE, WEAK), rank(new BalancedStrategy(), UNSORTED));
    }

    @Test
    void availabilityFirstRanksByAvailabilityBeforeTotal() {
        assertEquals(List.of(STRONG_AVAILABILITY, BEST_OVERALL, STRONG_COURSE, WEAK),
                rank(new AvailabilityFirstStrategy(), UNSORTED));
    }

    @Test
    void courseFirstRanksByCourseBeforeTotal() {
        assertEquals(List.of(STRONG_COURSE, BEST_OVERALL, STRONG_AVAILABILITY, WEAK), rank(new CourseFirstStrategy(), UNSORTED));
    }

    @Test
    void tiesOnTheFirstCriterionAreBrokenByTotal() {
        MatchScore higherTotal = score(1.0, 0.0, 0.9);
        MatchScore lowerTotal = score(1.0, 0.0, 0.3);

        assertEquals(List.of(higherTotal, lowerTotal), rank(new CourseFirstStrategy(), List.of(lowerTotal, higherTotal)));
    }

    @Test
    void aStrategyDoesNotWeightTheCriterionItRanksByFirst() {
        assertEquals(EnumSet.allOf(MatchingCriterion.class), new BalancedStrategy().weightedCriteria());
        assertFalse(new AvailabilityFirstStrategy().weightedCriteria().contains(MatchingCriterion.AVAILABILITY));
        assertEquals(4, new AvailabilityFirstStrategy().weightedCriteria().size());
        assertFalse(new CourseFirstStrategy().weightedCriteria().contains(MatchingCriterion.COURSE));
        assertEquals(4, new CourseFirstStrategy().weightedCriteria().size());
    }

    @Test
    void eachStrategyReportsItsOwnType() {
        assertEquals(MatchingStrategyType.BALANCED, new BalancedStrategy().type());
        assertEquals(MatchingStrategyType.AVAILABILITY_FIRST, new AvailabilityFirstStrategy().type());
        assertEquals(MatchingStrategyType.COURSE_FIRST, new CourseFirstStrategy().type());
    }
}
