package com.studybuddy.achievement;

import static org.assertj.core.api.Assertions.assertThat;

import com.studybuddy.achievement.AchievementProperties.TierThresholds;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class AchievementAssemblerTest {
    private final TierThresholds thresholds = new TierThresholds(1, 3, 5);
    private final StreakDto streak = new StreakDto(0, 0, List.of(), "Asia/Singapore");

    private AchievementAssembler assembler(int pointsPerLevel) {
        return new AchievementAssembler(new AchievementProperties(ZoneId.of("Asia/Singapore"), pointsPerLevel,
                thresholds, thresholds, thresholds, thresholds, thresholds, thresholds, thresholds));
    }

    private List<BadgeDto> badges(long count) {
        return Arrays.stream(BadgeDefinition.values())
                .map(definition -> new BadgeEvaluator().evaluate(definition, count, thresholds, null)).toList();
    }

    @Test
    void allGoldCapsTheLevelWithoutPromisingAnotherLevel() {
        AchievementsDto result = assembler(3).toDto(badges(10), streak);
        assertThat(result.level()).isEqualTo(8).isEqualTo(result.maxLevel());
        assertThat(result.points()).isEqualTo(21).isEqualTo(result.maxPoints());
        assertThat(result.pointsToNextLevel()).isZero();
    }

    @Test
    void finalLevelIsReachableWhenPointsPerLevelDoesNotDivideTheCatalogue() {
        AchievementsDto result = assembler(4).toDto(badges(10), streak);
        assertThat(result.level()).isEqualTo(7).isEqualTo(result.maxLevel());
        assertThat(result.pointsToNextLevel()).isZero();

        var almost = new java.util.ArrayList<>(badges(10));
        almost.set(0, new BadgeEvaluator().evaluate(BadgeDefinition.CONNECTOR, 3, thresholds, null));
        AchievementsDto before = assembler(4).toDto(almost, streak);
        assertThat(before.level()).isEqualTo(6);
        assertThat(before.pointsToNextLevel()).isEqualTo(1);
    }

    @Test
    void largeLevelStepStillTargetsAnAttainableFinalLevel() {
        AchievementsDto result = assembler(Integer.MAX_VALUE).toDto(badges(0), streak);
        assertThat(result.level()).isEqualTo(1);
        assertThat(result.maxLevel()).isEqualTo(2);
        assertThat(result.pointsToNextLevel()).isEqualTo(21);
    }
}
