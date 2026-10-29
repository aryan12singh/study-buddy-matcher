package com.studybuddy.achievement;

/**
 * One badge's standing. {@code tier} is the highest tier ever reached (null while locked) and is
 * never lowered; {@code currentTier} is what today's count alone reaches and can fall.
 * {@code target} is the count needed for the next tier above {@code tier}, or for gold once gold is earned.
 */
public record BadgeDto(
        String key,
        String title,
        String description,
        BadgeTier tier,
        BadgeTier currentTier,
        BadgeTier nextTier,
        long current,
        long target,
        boolean earned,
        String earnedHint) {
}
