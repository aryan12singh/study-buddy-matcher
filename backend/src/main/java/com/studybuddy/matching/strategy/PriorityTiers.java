package com.studybuddy.matching.strategy;

import java.util.List;

/**
 * The band boundaries that priority strategies rank by, bound from
 * {@code app.matching.priority-tiers.*}. Each list holds the lower bound of every
 * band except the last, best band first, so {@code [3, 1]} hours means "3 h or
 * more", "1 to 3 h" and "under 1 h".
 *
 * @param availabilityHours shared hours per week that start each Availability-first band
 * @param courseScore       course scores that start each Course-first band
 */
public record PriorityTiers(List<Double> availabilityHours, List<Double> courseScore) {

    public PriorityTiers {
        availabilityHours = descending(availabilityHours, "Availability tiers");
        courseScore = descending(courseScore, "Course tiers");
    }

    private static List<Double> descending(List<Double> floors, String label) {
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
