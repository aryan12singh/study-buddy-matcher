package com.studybuddy.matching.scoring;

import com.studybuddy.matching.MatchSearchCriteria;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Runs every {@link CriterionScorer} on a pair of students and adds up the
 * results, each multiplied by its weight. The weights add up to 1, so the total
 * is between 0 and 1. A criterion with no scorer counts as zero.
 */
@Component
public class WeightedMatchScorer {

    private final List<CriterionScorer> scorers;

    public WeightedMatchScorer(List<CriterionScorer> scorers) {
        this.scorers = List.copyOf(scorers);
    }

    public MatchScore score(MatchingProfile searcher, MatchingProfile candidate, MatchSearchCriteria search,
            MatchingWeights weights) {
        Map<MatchingCriterion, Double> breakdown = new EnumMap<>(MatchingCriterion.class);
        double weightedSum = 0.0;
        for (CriterionScorer scorer : scorers) {
            double score = scorer.score(searcher, candidate, search);
            breakdown.put(scorer.criterion(), score);
            weightedSum += weights.weightOf(scorer.criterion()) * score;
        }
        // Weights may add up to slightly more than 1 (see SUM_TOLERANCE), so keep the total in range.
        return new MatchScore(breakdown, Math.min(1.0, weightedSum));
    }
}
