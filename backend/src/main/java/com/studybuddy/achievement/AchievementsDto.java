package com.studybuddy.achievement;

import java.util.List;

/**
 * Everything the achievements screen shows for one student: level and points progress, the study
 * streak, and every badge with the tier earned so far. Seen only by that student.
 */
public record AchievementsDto(
        int level,
        int maxLevel,
        int points,
        int maxPoints,
        int pointsToNextLevel,
        int pointsPerLevel,
        int earnedCount,
        int totalCount,
        StreakDto streak,
        List<BadgeDto> badges) {
}
