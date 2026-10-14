package com.studybuddy.matching.strategy;

import com.studybuddy.matching.MatchQuality;
import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.studybuddy.matching.MatchingFixtures.tiers;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchingStrategyRankingTest {

    private static final Set<MatchingCriterion> ALL = EnumSet.allOf(MatchingCriterion.class);
    private final AvailabilityFirstStrategy availabilityFirst = new AvailabilityFirstStrategy(tiers());
    private final CourseFirstStrategy courseFirst = new CourseFirstStrategy(tiers());

    private static MatchScore score(String name, double course, double sharedHours, double total) {
        return new MatchScore(Map.of(MatchingCriterion.COURSE, course), total, sharedHours, name, MatchQuality.FAIR, Set.of());
    }

    private static List<String> rank(MatchingStrategy strategy, Set<MatchingCriterion> enabled, MatchScore... scores) {
        return List.of(scores).stream().sorted(strategy.ranking(enabled)).map(MatchScore::explanation).toList();
    }

    @Test
    void balancedRanksByTotal() {
        assertEquals(List.of("best", "middle", "worst"), rank(new BalancedStrategy(), ALL,
                score("middle", 1.0, 0, 0.5), score("worst", 0, 5, 0.1), score("best", 0, 0, 0.9)));
    }

    @Test
    void availabilityFirstRanksBySharedHourBandsBeforeTheTotal() {
        assertEquals(List.of("3.5h", "1.2h strong", "2.5h", "no shared time"), rank(availabilityFirst, ALL,
                score("2.5h", 0, 2.5, 0.60), score("no shared time", 1.0, 0, 0.99),
                score("3.5h", 0, 3.5, 0.40), score("1.2h strong", 1.0, 1.2, 0.95)));
    }

    @Test
    void aFewMoreMinutesInTheSameBandDoNotOverrideABetterScore() {
        // Before bands, any extra overlap at all ranked first regardless of every other criterion.
        assertEquals(List.of("1.25h strong", "2.75h weak"), rank(availabilityFirst, ALL,
                score("2.75h weak", 0, 2.75, 0.45), score("1.25h strong", 1.0, 1.25, 0.85)));
    }

    @Test
    void bandBoundariesAreInclusive() {
        assertEquals(2, availabilityFirst.tierOf(score("exactly 3h", 0, 3.0, 0)));
        assertEquals(1, availabilityFirst.tierOf(score("exactly 1h", 0, 1.0, 0)));
        assertEquals(0, availabilityFirst.tierOf(score("just under 1h", 0, 0.99, 0)));
    }

    @Test
    void courseFirstRanksByCourseBandsBeforeTheTotal() {
        assertEquals(List.of("wants a buddy for it", "takes it", "unrelated"), rank(courseFirst, ALL,
                score("unrelated", 0, 6, 0.99), score("takes it", 0.5, 0, 0.9), score("wants a buddy for it", 1.0, 0, 0.3)));
    }

    @Test
    void withinABandTheDisplayedScoreNeverIncreasesDownTheList() {
        List<MatchScore> ranked = List.of(score("a", 0, 1.5, 0.7), score("b", 0, 2.5, 0.9), score("c", 0, 1.0, 0.8))
                .stream().sorted(availabilityFirst.ranking(ALL)).toList();

        assertEquals(List.of(0.9, 0.8, 0.7), ranked.stream().map(MatchScore::total).toList());
    }

    @Test
    void disabledPriorityFallsBackToTotal() {
        MatchScore[] scores = {score("low", 1.0, 5, 0.2), score("high", 0, 0, 0.8)};

        assertEquals(List.of("high", "low"), rank(availabilityFirst, EnumSet.of(MatchingCriterion.COURSE), scores));
        assertEquals(List.of("high", "low"), rank(courseFirst, EnumSet.of(MatchingCriterion.AVAILABILITY), scores));
    }

    @Test
    void eachStrategyReportsItsOwnType() {
        assertEquals(MatchingStrategyType.BALANCED, new BalancedStrategy().type());
        assertEquals(MatchingStrategyType.AVAILABILITY_FIRST, availabilityFirst.type());
        assertEquals(MatchingStrategyType.COURSE_FIRST, courseFirst.type());
    }
}
