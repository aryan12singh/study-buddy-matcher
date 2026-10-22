package com.studybuddy.achievement;

import static org.assertj.core.api.Assertions.assertThat;

import com.studybuddy.achievement.AchievementProperties.TierThresholds;
import org.junit.jupiter.api.Test;

class BadgeEvaluatorTest {
    private static final TierThresholds THRESHOLDS = new TierThresholds(1, 3, 5);
    private final BadgeEvaluator evaluator = new BadgeEvaluator();

    @Test
    void lockedBadgeTargetsBronze() {
        BadgeDto badge = evaluator.evaluate(BadgeDefinition.CONNECTOR, 0, THRESHOLDS);
        assertThat(badge.earned()).isFalse();
        assertThat(badge.tier()).isNull();
        assertThat(badge.nextTier()).isEqualTo(BadgeTier.BRONZE);
        assertThat(badge.target()).isEqualTo(1);
        assertThat(badge.earnedHint()).isEqualTo("1 more connection to reach Bronze.");
    }

    @Test
    void silverBadgeTargetsGold() {
        BadgeDto badge = evaluator.evaluate(BadgeDefinition.CONNECTOR, 3, THRESHOLDS);
        assertThat(badge.earned()).isTrue();
        assertThat(badge.tier()).isEqualTo(BadgeTier.SILVER);
        assertThat(badge.nextTier()).isEqualTo(BadgeTier.GOLD);
        assertThat(badge.target()).isEqualTo(5);
        assertThat(badge.earnedHint()).isEqualTo("2 more connections to reach Gold.");
    }

    @Test
    void goldBadgeHasNoNextTier() {
        BadgeDto badge = evaluator.evaluate(BadgeDefinition.GROVE_KEEPER, 9, THRESHOLDS);
        assertThat(badge.tier()).isEqualTo(BadgeTier.GOLD);
        assertThat(badge.nextTier()).isNull();
        assertThat(badge.target()).isEqualTo(5);
    }
}
