package com.studybuddy.matching.scoring;

import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.matching.QualityThresholds;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Scores enabled criteria only; immutable results explain the exact weighted compatibility. */
@Component
public class WeightedMatchScorer {
    private final List<CriterionScorer> scorers;

    public WeightedMatchScorer(List<CriterionScorer> scorers) { this.scorers = List.copyOf(scorers); }

    public MatchScore score(MatchingProfile searcher, MatchingProfile candidate, MatchSearchCriteria search,
            MatchingWeights weights) {
        Map<MatchingCriterion, Double> breakdown = breakdown(searcher, candidate, search,
                EnumSet.allOf(MatchingCriterion.class));
        return new MatchScore(breakdown, total(breakdown, weights));
    }

    public MatchScore score(MatchingProfile searcher, MatchingProfile candidate, MatchSearchCriteria search,
            MatchingWeights weights, Set<MatchingCriterion> enabled, QualityThresholds thresholds) {
        Map<MatchingCriterion, Double> breakdown = breakdown(searcher, candidate, search, enabled);
        double total = total(breakdown, weights);
        double hours = searcher.availability().sharedMinutesWith(candidate.availability()) / 60.0;
        var quality = thresholds.classify(total);
        String criteria = Arrays.stream(MatchingCriterion.values()).filter(enabled::contains)
                .map(criterion -> criterion.label() + " " + String.format(Locale.ROOT, "%.1f%%", breakdown.get(criterion) * 100))
                .collect(Collectors.joining("; "));
        String explanation = quality.name().toLowerCase(Locale.ROOT) + " match: " + criteria
                + String.format(Locale.ROOT, ". Shared availability: %.1f hours/week. ", hours)
                + "Compatibility is not a success probability.";
        return new MatchScore(breakdown, total, hours, explanation, quality);
    }

    private Map<MatchingCriterion, Double> breakdown(MatchingProfile searcher, MatchingProfile candidate,
            MatchSearchCriteria search, Set<MatchingCriterion> enabled) {
        Map<MatchingCriterion, Double> breakdown = new EnumMap<>(MatchingCriterion.class);
        for (MatchingCriterion criterion : MatchingCriterion.values()) { breakdown.put(criterion, 0.0); }
        for (CriterionScorer scorer : scorers) {
            if (enabled.contains(scorer.criterion())) {
                breakdown.put(scorer.criterion(), scorer.score(searcher, candidate, search));
            }
        }
        return breakdown;
    }

    private static double total(Map<MatchingCriterion, Double> breakdown, MatchingWeights weights) {
        double sum = breakdown.entrySet().stream()
                .mapToDouble(entry -> weights.weightOf(entry.getKey()) * entry.getValue()).sum();
        return Math.min(1.0, sum);
    }
}
