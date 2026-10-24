package com.studybuddy.achievement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studybuddy.achievement.AchievementProperties.TierThresholds;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AchievementPropertiesTest {
    @Test
    void rejectsZeroNegativeAndUnorderedThresholds() {
        assertThatThrownBy(() -> new TierThresholds(0, 1, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TierThresholds(-1, 1, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TierThresholds(2, 1, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TierThresholds(1, 3, 2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsZeroLevelStepBeforeAnyDivision() {
        TierThresholds thresholds = new TierThresholds(1, 3, 5);
        assertThatThrownBy(() -> new AchievementProperties(ZoneId.of("Asia/Singapore"), 0,
                thresholds, thresholds, thresholds, thresholds, thresholds, thresholds))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
