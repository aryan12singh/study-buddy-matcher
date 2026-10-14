package com.studybuddy.matching.scoring;

import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.matching.QualityThresholds;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Scores enabled criteria only and combines them into the weighted total and quality label. */
@Component
public class WeightedMatchScorer {

    private static final double MINUTES_PER_HOUR = 60.0;

    private final List<CriterionScorer> scorers;
    private final QualityThresholds thresholds;
    private final ScoringRules rules;

    public WeightedMatchScorer(List<CriterionScorer> scorers, QualityThresholds thresholds, ScoringRules rules) {
        this.scorers = List.copyOf(scorers);
        this.thresholds = thresholds;
        this.rules = rules;
    }

    public MatchScore score(MatchingProfile searcher, MatchingProfile candidate, MatchSearchCriteria search,
            MatchingWeights weights, Set<MatchingCriterion> enabled) {
        Map<MatchingCriterion, Double> breakdown = new EnumMap<>(MatchingCriterion.class);
        Set<MatchingCriterion> unknown = EnumSet.noneOf(MatchingCriterion.class);
        for (MatchingCriterion criterion : MatchingCriterion.values()) { breakdown.put(criterion, 0.0); }
        for (CriterionScorer scorer : scorers) {
            if (enabled.contains(scorer.criterion())) {
                CriterionResult result = scorer.score(searcher, candidate, search);
                breakdown.put(scorer.criterion(), result.known() ? result.score() : rules.unknownScore());
                if (!result.known()) { unknown.add(scorer.criterion()); }
            }
        }
        double total = total(breakdown, weights);
        double hours = searcher.availability().sessionMinutesWith(candidate.availability(), rules.minSessionMinutes()) / MINUTES_PER_HOUR;
        return new MatchScore(breakdown, total, hours, thresholds.classify(total), unknown);
    }

    private static double total(Map<MatchingCriterion, Double> breakdown, MatchingWeights weights) {
        double sum = breakdown.entrySet().stream()
                .mapToDouble(entry -> weights.weightOf(entry.getKey()) * entry.getValue()).sum();
        return Math.min(1.0, sum);
    }
}
