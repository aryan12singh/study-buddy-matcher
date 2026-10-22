package com.studybuddy.achievement;

import com.studybuddy.achievement.AchievementProperties.TierThresholds;

/** Pure rule: maps a count onto bronze, silver and gold thresholds. */
public final class BadgeEvaluator {

    public BadgeDto evaluate(BadgeDefinition definition, long current, TierThresholds thresholds) {
        BadgeTier tier = reachedTier(current, thresholds);
        BadgeTier next = tier == null ? BadgeTier.BRONZE : tier == BadgeTier.GOLD ? null : BadgeTier.values()[tier.ordinal() + 1];
        long target = threshold(next == null ? BadgeTier.GOLD : next, thresholds);
        String hint = next == null
                ? "Gold reached. Every tier earned."
                : definition.hint(target - current, next);
        return new BadgeDto(definition.key(), definition.title(), definition.description(),
                tier, next, current, target, tier != null, hint);
    }

    private static BadgeTier reachedTier(long current, TierThresholds thresholds) {
        if (current >= thresholds.gold()) return BadgeTier.GOLD;
        if (current >= thresholds.silver()) return BadgeTier.SILVER;
        if (current >= thresholds.bronze()) return BadgeTier.BRONZE;
        return null;
    }

    private static long threshold(BadgeTier tier, TierThresholds thresholds) {
        return switch (tier) {
            case BRONZE -> thresholds.bronze();
            case SILVER -> thresholds.silver();
            case GOLD -> thresholds.gold();
        };
    }
}
