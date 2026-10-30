package com.studybuddy.achievement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.ZoneId;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Badge thresholds and streak settings, bound from {@code app.achievements.*}. */
@Validated
@ConfigurationProperties("app.achievements")
public record AchievementProperties(
        @NotNull ZoneId weekZone,
        @Min(1) int pointsPerLevel,
        @Valid @NotNull TierThresholds connections,
        @Valid @NotNull TierThresholds requestsSent,
        @Valid @NotNull TierThresholds groupsJoined,
        @Valid @NotNull TierThresholds groupsLed,
        @Valid @NotNull TierThresholds sessionsPlanned,
        @Valid @NotNull TierThresholds streakWeeks,
        @Valid @NotNull TierThresholds treesPlanted) {

    public AchievementProperties {
        Objects.requireNonNull(weekZone, "Week zone is required");
        if (pointsPerLevel < 1) throw new IllegalArgumentException("Points per level must be positive");
        Objects.requireNonNull(connections, "Connection thresholds are required");
        Objects.requireNonNull(requestsSent, "Request thresholds are required");
        Objects.requireNonNull(groupsJoined, "Joined-group thresholds are required");
        Objects.requireNonNull(groupsLed, "Created-group thresholds are required");
        Objects.requireNonNull(sessionsPlanned, "Session thresholds are required");
        Objects.requireNonNull(streakWeeks, "Streak thresholds are required");
        Objects.requireNonNull(treesPlanted, "Tree thresholds are required");
    }

    /** The counts at which a badge reaches bronze, silver and gold, in increasing order. */
    public record TierThresholds(@Min(1) int bronze, @Min(1) int silver, @Min(1) int gold) {
        public TierThresholds {
            if (!(bronze > 0 && bronze <= silver && silver <= gold)) {
                throw new IllegalArgumentException("Tier thresholds must be positive and bronze <= silver <= gold");
            }
        }
    }
}
