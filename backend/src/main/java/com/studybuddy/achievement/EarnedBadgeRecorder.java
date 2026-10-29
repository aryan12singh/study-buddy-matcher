package com.studybuddy.achievement;

import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Keeps the highest tier each badge has reached, so an earned tier survives a falling count. */
@Component
public class EarnedBadgeRecorder {
    private final EarnedBadgeRepository earnedBadges;

    public EarnedBadgeRecorder(EarnedBadgeRepository earnedBadges) {
        this.earnedBadges = earnedBadges;
    }

    /**
     * Saves every live tier that is above the stored one, then returns the highest tier per badge.
     * Badges never earned are absent from the result.
     */
    public Map<BadgeDefinition, BadgeTier> keepHighest(Long studentId, Map<BadgeDefinition, BadgeTier> liveTiers) {
        Map<BadgeDefinition, BadgeTier> best = new EnumMap<>(BadgeDefinition.class);
        for (EarnedBadge saved : earnedBadges.findByStudentId(studentId)) {
            best.put(saved.getBadge(), saved.getBestTier());
        }
        liveTiers.forEach((badge, live) -> {
            if (live != null && live.isAbove(best.get(badge))) {
                earnedBadges.raise(studentId, badge.name(), live.name());
                best.put(badge, live);
            }
        });
        return best;
    }
}
