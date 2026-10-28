package com.studybuddy.achievement;

import com.studybuddy.achievement.AchievementProperties.TierThresholds;

/** Pure rule: maps a count onto bronze, silver and gold thresholds, keeping any tier already earned. */
public final class BadgeEvaluator {

    /** The tier a count reaches on its own, or null below bronze. */
    public BadgeTier tierFor(long current, TierThresholds thresholds) {
        if (current >= thresholds.gold()) return BadgeTier.GOLD;
        if (current >= thresholds.silver()) return BadgeTier.SILVER;
        if (current >= thresholds.bronze()) return BadgeTier.BRONZE;
        return null;
    }

    /**
     * Builds a badge from today's count and the highest tier saved earlier (null if none).
     * The shown tier is the higher of the two, and progress points at the next tier above it.
     */
    public BadgeDto evaluate(BadgeDefinition definition, long current, TierThresholds thresholds, BadgeTier savedTier) {
        BadgeTier currentTier = tierFor(current, thresholds);
        BadgeTier tier = BadgeTier.higher(currentTier, savedTier);
        BadgeTier next = tier == null ? BadgeTier.BRONZE : tier == BadgeTier.GOLD ? null : BadgeTier.values()[tier.ordinal() + 1];
        long target = threshold(next == null ? BadgeTier.GOLD : next, thresholds);
        String hint = next == null
                ? "Gold earned. No higher tier."
                : definition.hint(current, target, next);
        return new BadgeDto(definition.key(), definition.title(), definition.description(),
                tier, currentTier, next, current, target, tier != null, hint);
    }

    private static long threshold(BadgeTier tier, TierThresholds thresholds) {
        return switch (tier) {
            case BRONZE -> thresholds.bronze();
            case SILVER -> thresholds.silver();
            case GOLD -> thresholds.gold();
        };
    }
}
