package com.studybuddy.achievement;

import java.util.List;

public record AchievementsDto(
        int level,
        int points,
        int pointsToNextLevel,
        int pointsPerLevel,
        int earnedCount,
        int totalCount,
        StreakDto streak,
        List<BadgeDto> badges) {
}
