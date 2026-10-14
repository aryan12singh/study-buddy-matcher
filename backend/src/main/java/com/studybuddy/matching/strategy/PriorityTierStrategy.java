package com.studybuddy.matching.strategy;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Ranks by one priority criterion before anything else, as Appendix A describes, but
 * in coarse bands rather than by its exact value. Within a band the weighted total
 * decides, so fifteen extra minutes of overlap cannot outweigh every other criterion.
 * Subclasses only say which criterion has priority and what value the bands measure.
 */
public abstract class PriorityTierStrategy implements MatchingStrategy {

    private final List<Double> tierFloors;

    protected PriorityTierStrategy(List<Double> tierFloors) {
        this.tierFloors = List.copyOf(tierFloors);
    }

    /** The criterion this strategy ranks by first. */
    protected abstract MatchingCriterion priority();

    /** The value the bands are drawn from, such as shared hours per week. */
    protected abstract double priorityValue(MatchScore score);

    /** The candidate's band: the number of band boundaries it reaches, so higher is better. */
    public int tierOf(MatchScore score) {
        double value = priorityValue(score);
        return (int) tierFloors.stream().filter(floor -> value >= floor).count();
    }

    @Override
    public Comparator<MatchScore> ranking(Set<MatchingCriterion> enabled) {
        Comparator<MatchScore> byTotal = Comparator.comparingDouble(MatchScore::total).reversed();
        if (!enabled.contains(priority())) {
            return byTotal;
        }
        return Comparator.comparingInt(this::tierOf).reversed().thenComparing(byTotal);
    }
}
