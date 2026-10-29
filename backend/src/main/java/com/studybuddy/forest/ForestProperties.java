package com.studybuddy.forest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.ZoneId;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Focus forest settings, bound from {@code app.forest.*}. */
@Validated
@ConfigurationProperties("app.forest")
public record ForestProperties(@NotNull ZoneId zone, @Valid @NotNull TreeThresholds treeMinutes) {
    public ForestProperties {
        Objects.requireNonNull(zone, "Forest zone is required");
        Objects.requireNonNull(treeMinutes, "Tree thresholds are required");
    }

    /** Focused minutes at which a visit's tree reaches each species, in increasing order. */
    public record TreeThresholds(int sprout, int sapling, int oak, int pine, int ancientBlossom) {
        public TreeThresholds {
            if (!(sprout > 0 && sprout < sapling && sapling < oak && oak < pine && pine < ancientBlossom)) {
                throw new IllegalArgumentException("Tree thresholds must be positive and strictly increasing");
            }
        }

        public int minutesFor(TreeSpecies species) {
            return switch (species) {
                case SPROUT -> sprout;
                case SAPLING -> sapling;
                case OAK -> oak;
                case PINE -> pine;
                case ANCIENT_BLOSSOM -> ancientBlossom;
            };
        }
    }
}
