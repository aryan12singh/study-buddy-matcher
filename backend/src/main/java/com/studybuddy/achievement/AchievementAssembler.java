package com.studybuddy.achievement;

import java.util.List;
import org.springframework.stereotype.Component;

/** Assembles the current badge standing and a level whose final step is always reachable. */
@Component
public class AchievementAssembler {
    private final AchievementProperties properties;

    public AchievementAssembler(AchievementProperties properties) {
        this.properties = properties;
    }

    public AchievementsDto toDto(List<BadgeDto> badges, StreakDto streak) {
        int points = badges.stream().filter(BadgeDto::earned).mapToInt(badge -> badge.tier().points()).sum();
        int maximum = badges.size() * BadgeTier.GOLD.points();
        int perLevel = properties.pointsPerLevel();
        int maxLevel = maximum == 0 ? 1 : (maximum - 1) / perLevel + 2;
        int level = points == maximum ? maxLevel : points / perLevel + 1;
        int remaining = Math.min(perLevel - points % perLevel, maximum - points);
        int earned = (int) badges.stream().filter(BadgeDto::earned).count();
        return new AchievementsDto(level, maxLevel, points, maximum, remaining, perLevel,
                earned, badges.size(), streak, List.copyOf(badges));
    }
}
