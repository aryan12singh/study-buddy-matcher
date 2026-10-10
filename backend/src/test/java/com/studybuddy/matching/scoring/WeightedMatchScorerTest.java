package com.studybuddy.matching.scoring;

import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.matching.availability.WeeklyAvailability;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.studybuddy.matching.MatchingFixtures.evenWeights;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WeightedMatchScorerTest {

    private static final MatchingProfile SELF = new MatchingProfile(null, new WeeklyAvailability(List.of()));
    private static final MatchingProfile CANDIDATE = new MatchingProfile(null, new WeeklyAvailability(List.of()));
    private static final MatchSearchCriteria SEARCH = new MatchSearchCriteria(1L, null, null, null, null, null);

    /** A scorer that always returns the same score, so the weighting can be checked on its own. */
    private record FixedScorer(MatchingCriterion criterion, double fixedScore) implements CriterionScorer {
        @Override
        public double score(MatchingProfile searcher, MatchingProfile candidate, MatchSearchCriteria search) {
            return fixedScore;
        }
    }

    private static List<CriterionScorer> allCriteriaScoring(double score) {
        return List.of(MatchingCriterion.values()).stream()
                .<CriterionScorer>map(criterion -> new FixedScorer(criterion, score))
                .toList();
    }

    @Test
    void totalIsTheWeightedSumOfTheCriterionScores() {
        Map<MatchingCriterion, Double> values = evenWeights();
        values.put(MatchingCriterion.COURSE, 0.6);
        values.put(MatchingCriterion.STUDY_MODE, 0.1);
        values.put(MatchingCriterion.STUDY_GOAL, 0.05);
        values.put(MatchingCriterion.GROUP_SIZE, 0.05);
        WeightedMatchScorer scorer = new WeightedMatchScorer(List.of(
                new FixedScorer(MatchingCriterion.COURSE, 1.0),
                new FixedScorer(MatchingCriterion.AVAILABILITY, 0.5),
                new FixedScorer(MatchingCriterion.STUDY_MODE, 0.0),
                new FixedScorer(MatchingCriterion.STUDY_GOAL, 0.0),
                new FixedScorer(MatchingCriterion.GROUP_SIZE, 0.0)));

        MatchScore score = scorer.score(SELF, CANDIDATE, SEARCH, new MatchingWeights(values));

        // 0.6 * 1.0 + 0.2 * 0.5 + 0 + 0 + 0
        assertEquals(0.7, score.total(), 1e-9);
        assertEquals(1.0, score.scoreFor(MatchingCriterion.COURSE));
        assertEquals(0.5, score.scoreFor(MatchingCriterion.AVAILABILITY));
    }

    @Test
    void perfectScoresGiveATotalOfExactlyOne() {
        Map<MatchingCriterion, Double> values = evenWeights();
        values.put(MatchingCriterion.COURSE, 0.1);
        values.put(MatchingCriterion.AVAILABILITY, 0.5);
        values.put(MatchingCriterion.STUDY_GOAL, 0.1);
        values.put(MatchingCriterion.GROUP_SIZE, 0.1);
        WeightedMatchScorer scorer = new WeightedMatchScorer(allCriteriaScoring(1.0));

        assertEquals(1.0, scorer.score(SELF, CANDIDATE, SEARCH, new MatchingWeights(values)).total(), 1e-9);
    }

    @Test
    void criterionWithNoScorerCountsAsZero() {
        WeightedMatchScorer scorer = new WeightedMatchScorer(List.of(new FixedScorer(MatchingCriterion.COURSE, 1.0)));

        MatchScore score = scorer.score(SELF, CANDIDATE, SEARCH, new MatchingWeights(evenWeights()));

        assertEquals(0.2, score.total(), 1e-9);
        assertEquals(0.0, score.scoreFor(MatchingCriterion.AVAILABILITY));
    }
}
