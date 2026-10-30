package com.studybuddy.forest;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeLotteryTest {
    private final ForestProperties properties = new ForestProperties(ZoneId.of("Asia/Singapore"),
        new ForestProperties.TreeThresholds(10, 25, 50, 90, 150), 3,
        new ForestProperties.LightHours(5, 11, 17, 21), 100);

    @Test
    void drawsEveryVariantAndPlotsWithinRange() {
        TreeLottery lottery = new TreeLottery(new Random(42), properties);
        Set<Integer> variants = new HashSet<>();
        for (int i = 0; i < 300; i++) {
            TreeDraw draw = lottery.draw();
            variants.add(draw.variant());
            assertTrue(draw.plot() >= 0 && draw.plot() < TreeLottery.PLOT_RANGE);
        }
        assertEquals(Set.of(0, 1, 2), variants);
    }

    @Test
    void theSameSeedDrawsTheSameForestSoTestsCanPinIt() {
        assertEquals(new TreeLottery(new Random(7), properties).draw(), new TreeLottery(new Random(7), properties).draw());
    }
}
