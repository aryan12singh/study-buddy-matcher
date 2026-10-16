package com.studybuddy.matching.strategy;

import java.util.List;

/**
 * The band boundaries for Availability-first, bound from
 * {@code app.matching.priority-tiers.*}. The list holds the lower bound of every band
 * except the last, best band first, so {@code [3, 1]} hours means "3 h or more",
 * "1 to 3 h" and "under 1 h". Course-first has no setting here: its bands come from the
 * course scoring rule itself (see {@link CourseFirstStrategy}).
 *
 * @param availabilityHours shared hours per week that start each Availability-first band
 */
public record PriorityTiers(List<Double> availabilityHours) {

    public PriorityTiers {
        availabilityHours = PriorityTierStrategy.requireDescending(availabilityHours, "Availability tiers");
    }
}
