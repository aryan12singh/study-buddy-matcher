package com.studybuddy.forest;

import java.util.Optional;
import org.springframework.stereotype.Component;

/** Maps focused minutes to a tree species using the configured thresholds. */
@Component
public class TreeGrowth {
    private final ForestProperties.TreeThresholds thresholds;

    public TreeGrowth(ForestProperties properties) {
        this.thresholds = properties.treeMinutes();
    }

    /** The species a visit has grown, or empty when it is still too short to plant anything. */
    public Optional<TreeSpecies> speciesFor(long focusedMinutes) {
        TreeSpecies grown = null;
        for (TreeSpecies species : TreeSpecies.values()) {
            if (focusedMinutes >= thresholds.minutesFor(species)) grown = species;
        }
        return Optional.ofNullable(grown);
    }

    /** The next species to grow into, or empty once the grandest one is reached. */
    public Optional<TreeSpecies> nextAfter(long focusedMinutes) {
        for (TreeSpecies species : TreeSpecies.values()) {
            if (focusedMinutes < thresholds.minutesFor(species)) return Optional.of(species);
        }
        return Optional.empty();
    }

    public int minutesFor(TreeSpecies species) {
        return thresholds.minutesFor(species);
    }

    public int minimumMinutes() {
        return thresholds.sprout();
    }
}
