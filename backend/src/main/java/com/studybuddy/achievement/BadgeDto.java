package com.studybuddy.achievement;

/**
 * One badge's current standing. {@code tier} is the highest eligible tier (null while locked);
 * {@code target} is the count needed for the next tier, or for gold once gold is reached.
 */
public record BadgeDto(
        String key,
        String title,
        String description,
        BadgeTier tier,
        BadgeTier nextTier,
        long current,
        long target,
        boolean earned,
        String earnedHint) {
}
