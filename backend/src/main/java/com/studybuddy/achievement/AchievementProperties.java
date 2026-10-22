package com.studybuddy.achievement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.ZoneId;
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
        @Valid @NotNull TierThresholds streakWeeks) {

    /** The counts at which a badge reaches bronze, silver and gold, in increasing order. */
    public record TierThresholds(@Min(1) int bronze, @Min(1) int silver, @Min(1) int gold) {
        public TierThresholds {
            if (!(bronze <= silver && silver <= gold)) {
                throw new IllegalArgumentException("Tier thresholds must be bronze <= silver <= gold");
            }
        }
    }
}
