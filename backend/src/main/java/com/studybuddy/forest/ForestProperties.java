package com.studybuddy.forest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.ZoneId;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Focus forest settings, bound from {@code app.forest.*}. */
@Validated
@ConfigurationProperties("app.forest")
public record ForestProperties(@NotNull ZoneId zone, @Valid @NotNull TreeThresholds treeMinutes,
    @Min(1) int variantsPerSpecies, @Valid @NotNull LightHours lightStarts, @Min(1) int updateQueueCapacity) {
    public ForestProperties {
        Objects.requireNonNull(zone, "Forest zone is required");
        Objects.requireNonNull(treeMinutes, "Tree thresholds are required");
        Objects.requireNonNull(lightStarts, "Light hours are required");
        if (variantsPerSpecies < 1) throw new IllegalArgumentException("Each species needs at least one variant");
        if (updateQueueCapacity < 1) throw new IllegalArgumentException("The forest update queue needs room");
    }

    /** The local hour each light begins; night runs on past midnight until dawn. */
    public record LightHours(int dawn, int day, int dusk, int night) {
        public LightHours {
            if (!(dawn >= 0 && dawn < day && day < dusk && dusk < night && night <= 23)) {
                throw new IllegalArgumentException("Light hours must rise within one day");
            }
        }
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
