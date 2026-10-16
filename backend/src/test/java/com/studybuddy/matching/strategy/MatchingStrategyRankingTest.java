package com.studybuddy.matching.strategy;

import com.studybuddy.matching.MatchQuality;
import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.scoring.ScoringRules;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.studybuddy.matching.MatchingFixtures.rules;
import static com.studybuddy.matching.MatchingFixtures.tiers;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchingStrategyRankingTest {

    private static final Set<MatchingCriterion> ALL = EnumSet.allOf(MatchingCriterion.class);
    private final AvailabilityFirstStrategy availabilityFirst = new AvailabilityFirstStrategy(tiers());
    private final CourseFirstStrategy courseFirst = new CourseFirstStrategy(rules());

    /** A score with a name, so a ranking can be read as a list of names. */
    private record Named(String name, MatchScore score) {
    }

    private static Named score(String name, double course, double sharedHours, double total) {
        return new Named(name, new MatchScore(Map.of(MatchingCriterion.COURSE, course), total, sharedHours, MatchQuality.FAIR, Set.of()));
    }

    private static List<String> rank(MatchingStrategy strategy, Set<MatchingCriterion> enabled, Named... scores) {
        return List.of(scores).stream().sorted(Comparator.comparing(Named::score, strategy.ranking(enabled))).map(Named::name).toList();
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
        assertEquals(2, availabilityFirst.tierOf(score("exactly 3h", 0, 3.0, 0).score()));
        assertEquals(1, availabilityFirst.tierOf(score("exactly 1h", 0, 1.0, 0).score()));
        assertEquals(0, availabilityFirst.tierOf(score("just under 1h", 0, 0.99, 0).score()));
    }

    @Test
    void courseFirstRanksByCourseBandsBeforeTheTotal() {
        assertEquals(List.of("wants a buddy for it", "takes it", "unrelated"), rank(courseFirst, ALL,
                score("unrelated", 0, 6, 0.99), score("takes it", 0.5, 0, 0.9), score("wants a buddy for it", 1.0, 0, 0.3)));
    }

    @Test
    void courseBandsFollowTheConfiguredTakesCourseScore() {
        // Course bands come from the scoring rule, so lowering the takes-course score cannot
        // drop "takes it" into the same band as "neither".
        CourseFirstStrategy lowerPartialScore = new CourseFirstStrategy(new ScoringRules(0.0, 0.4, 60));

        assertEquals(List.of("wants a buddy for it", "takes it", "unrelated"), rank(lowerPartialScore, ALL,
                score("unrelated", 0, 6, 0.60), score("takes it", 0.4, 0, 0.55), score("wants a buddy for it", 1.0, 0, 0.70)));
    }

    @Test
    void aTakesCourseScoreOfZeroOrOneLeavesOnlyTheFullMatchBand() {
        for (double takesCourse : new double[] {0.0, 1.0}) {
            CourseFirstStrategy strategy = new CourseFirstStrategy(new ScoringRules(0.0, takesCourse, 60));

            assertEquals(1, strategy.tierOf(score("wants a buddy for it", 1.0, 0, 0).score()));
            assertEquals(0, strategy.tierOf(score("partial", 0.5, 0, 0).score()));
        }
    }

    @Test
    void unknownPriorityAlwaysLandsInTheLowestBand() {
        // With an unknown score of 0.5, a student with no courses would otherwise share the
        // "half or more" band with a real match and outrank them on total.
        Named noCourses = new Named("no courses", new MatchScore(Map.of(MatchingCriterion.COURSE, 0.5), 0.9, 0,
                MatchQuality.STRONG, Set.of(MatchingCriterion.COURSE)));

        assertEquals(0, courseFirst.tierOf(noCourses.score()));
        assertEquals(List.of("shares half my courses", "no courses"), rank(courseFirst, ALL, noCourses,
                score("shares half my courses", 0.5, 0, 0.4)));
    }

    @Test
    void withinABandTheDisplayedScoreNeverIncreasesDownTheList() {
        List<MatchScore> ranked = List.of(score("a", 0, 1.5, 0.7), score("b", 0, 2.5, 0.9), score("c", 0, 1.0, 0.8))
                .stream().map(Named::score).sorted(availabilityFirst.ranking(ALL)).toList();

        assertEquals(List.of(0.9, 0.8, 0.7), ranked.stream().map(MatchScore::total).toList());
    }

    @Test
    void disabledPriorityFallsBackToTotal() {
        Named[] scores = {score("low", 1.0, 5, 0.2), score("high", 0, 0, 0.8)};

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
