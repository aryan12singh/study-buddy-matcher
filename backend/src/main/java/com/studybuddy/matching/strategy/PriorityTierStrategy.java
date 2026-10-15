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
 * A candidate whose priority criterion is unknown always lands in the lowest band, so
 * missing data never outranks real data whatever the configured unknown score is.
 * Subclasses only say which criterion has priority and what value the bands measure.
 */
public abstract class PriorityTierStrategy implements MatchingStrategy {

    private static final int LOWEST_TIER = 0;

    private final List<Double> tierFloors;

    protected PriorityTierStrategy(List<Double> tierFloors) {
        this.tierFloors = requireDescending(tierFloors, "Priority tiers");
    }

    /** The criterion this strategy ranks by first. */
    protected abstract MatchingCriterion priority();

    /** The value the bands are drawn from, such as shared hours per week. */
    protected abstract double priorityValue(MatchScore score);

    /** The candidate's band: the number of band boundaries it reaches, so higher is better. */
    public int tierOf(MatchScore score) {
        if (!score.isKnown(priority())) {
            return LOWEST_TIER;
        }
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

    /**
     * Checks band boundaries: at least one, all positive, strictly highest first, so
     * every band is distinct. Shared by configured and derived boundaries.
     */
    static List<Double> requireDescending(List<Double> floors, String label) {
        if (floors == null || floors.isEmpty()) {
            throw new IllegalArgumentException(label + " need at least one boundary");
        }
        for (int index = 0; index < floors.size(); index++) {
            Double floor = floors.get(index);
            if (floor == null || !Double.isFinite(floor) || floor <= 0.0) {
                throw new IllegalArgumentException(label + " must be positive numbers");
            }
            if (index > 0 && floor >= floors.get(index - 1)) {
                throw new IllegalArgumentException(label + " must be listed from highest to lowest");
            }
        }
        return List.copyOf(floors);
    }
}
