package com.studybuddy.achievement;

import static org.assertj.core.api.Assertions.assertThat;

import com.studybuddy.achievement.AchievementProperties.TierThresholds;
import org.junit.jupiter.api.Test;

class BadgeEvaluatorTest {
    private static final TierThresholds THRESHOLDS = new TierThresholds(1, 3, 5);
    private final BadgeEvaluator evaluator = new BadgeEvaluator();

    @Test
    void lockedBadgeTargetsBronze() {
        BadgeDto badge = evaluator.evaluate(BadgeDefinition.CONNECTOR, 0, THRESHOLDS, null);
        assertThat(badge.earned()).isFalse();
        assertThat(badge.tier()).isNull();
        assertThat(badge.nextTier()).isEqualTo(BadgeTier.BRONZE);
        assertThat(badge.target()).isEqualTo(1);
        assertThat(badge.earnedHint()).isEqualTo("1 more active connection to reach Bronze.");
    }

    @Test
    void silverBadgeTargetsGold() {
        BadgeDto badge = evaluator.evaluate(BadgeDefinition.CONNECTOR, 3, THRESHOLDS, null);
        assertThat(badge.earned()).isTrue();
        assertThat(badge.tier()).isEqualTo(BadgeTier.SILVER);
        assertThat(badge.nextTier()).isEqualTo(BadgeTier.GOLD);
        assertThat(badge.target()).isEqualTo(5);
        assertThat(badge.earnedHint()).isEqualTo("2 more active connections to reach Gold.");
    }

    @Test
    void goldBadgeHasNoNextTier() {
        BadgeDto badge = evaluator.evaluate(BadgeDefinition.GROVE_KEEPER, 9, THRESHOLDS, null);
        assertThat(badge.tier()).isEqualTo(BadgeTier.GOLD);
        assertThat(badge.nextTier()).isNull();
        assertThat(badge.target()).isEqualTo(5);
        assertThat(badge.earnedHint()).isEqualTo("Gold earned. No higher tier.");
    }

    @Test
    void groveHintStatesTheFullRunTargetBecauseTheCurrentRunMayHaveRestarted() {
        BadgeDto badge = evaluator.evaluate(BadgeDefinition.GROVE_KEEPER, 3, new TierThresholds(2, 4, 8), null);
        assertThat(badge.earnedHint()).isEqualTo("Plan 4 consecutive weeks to reach Silver. Your longest eligible run is 3.");
    }

    @Test
    void savedTierIsKeptWhenTheCountFallsAndProgressAimsAboveIt() {
        BadgeDto badge = evaluator.evaluate(BadgeDefinition.CONNECTOR, 1, THRESHOLDS, BadgeTier.SILVER);
        assertThat(badge.tier()).isEqualTo(BadgeTier.SILVER);
        assertThat(badge.currentTier()).isEqualTo(BadgeTier.BRONZE);
        assertThat(badge.earned()).isTrue();
        assertThat(badge.nextTier()).isEqualTo(BadgeTier.GOLD);
        assertThat(badge.target()).isEqualTo(5);
        assertThat(badge.earnedHint()).isEqualTo("4 more active connections to reach Gold.");
    }

    @Test
    void savedTierStillCountsWhenTheCountIsZero() {
        BadgeDto badge = evaluator.evaluate(BadgeDefinition.TEAM_PLAYER, 0, THRESHOLDS, BadgeTier.BRONZE);
        assertThat(badge.tier()).isEqualTo(BadgeTier.BRONZE);
        assertThat(badge.currentTier()).isNull();
        assertThat(badge.earned()).isTrue();
    }

    @Test
    void liveTierAboveTheSavedOneWins() {
        BadgeDto badge = evaluator.evaluate(BadgeDefinition.CONNECTOR, 5, THRESHOLDS, BadgeTier.BRONZE);
        assertThat(badge.tier()).isEqualTo(BadgeTier.GOLD);
        assertThat(badge.currentTier()).isEqualTo(BadgeTier.GOLD);
    }

    @Test
    void tierComparisonTreatsNullAsNotEarned() {
        assertThat(BadgeTier.BRONZE.isAbove(null)).isTrue();
        assertThat(BadgeTier.SILVER.isAbove(BadgeTier.GOLD)).isFalse();
        assertThat(BadgeTier.SILVER.isAbove(BadgeTier.SILVER)).isFalse();
        assertThat(BadgeTier.higher(null, BadgeTier.BRONZE)).isEqualTo(BadgeTier.BRONZE);
        assertThat(BadgeTier.higher(BadgeTier.GOLD, BadgeTier.SILVER)).isEqualTo(BadgeTier.GOLD);
        assertThat(BadgeTier.higher(null, null)).isNull();
    }
}
