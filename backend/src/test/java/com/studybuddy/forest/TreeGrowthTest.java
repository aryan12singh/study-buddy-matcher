package com.studybuddy.forest;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TreeGrowthTest {
    private final TreeGrowth growth = new TreeGrowth(new ForestProperties(ZoneId.of("Asia/Singapore"),
        new ForestProperties.TreeThresholds(10, 25, 50, 90, 150), 3,
        new ForestProperties.LightHours(5, 11, 17, 21), 100));

    @Test
    void longerVisitsGrowGranderTrees() {
        assertEquals(Optional.empty(), growth.speciesFor(9));
        assertEquals(Optional.of(TreeSpecies.SPROUT), growth.speciesFor(10));
        assertEquals(Optional.of(TreeSpecies.SAPLING), growth.speciesFor(25));
        assertEquals(Optional.of(TreeSpecies.OAK), growth.speciesFor(89));
        assertEquals(Optional.of(TreeSpecies.PINE), growth.speciesFor(90));
        assertEquals(Optional.of(TreeSpecies.ANCIENT_BLOSSOM), growth.speciesFor(600));
    }

    @Test
    void namesTheNextStageUntilTheGrandestTree() {
        assertEquals(Optional.of(TreeSpecies.SPROUT), growth.nextAfter(0));
        assertEquals(Optional.of(TreeSpecies.OAK), growth.nextAfter(25));
        assertEquals(Optional.empty(), growth.nextAfter(150));
        assertEquals(10, growth.minimumMinutes());
    }

    @Test
    void thresholdsMustRise() {
        assertThrows(IllegalArgumentException.class, () -> new ForestProperties.TreeThresholds(10, 10, 50, 90, 150));
        assertThrows(IllegalArgumentException.class, () -> new ForestProperties.TreeThresholds(0, 25, 50, 90, 150));
    }

    @Test
    void lightFollowsTheLocalStartTime() {
        var hours = new ForestProperties.LightHours(5, 11, 17, 21);
        assertEquals(TreeLight.DAWN, TreeLight.at(LocalTime.of(5, 0), hours));
        assertEquals(TreeLight.DAY, TreeLight.at(LocalTime.of(11, 0), hours));
        assertEquals(TreeLight.DUSK, TreeLight.at(LocalTime.of(20, 59), hours));
        assertEquals(TreeLight.NIGHT, TreeLight.at(LocalTime.of(21, 0), hours));
        assertEquals(TreeLight.NIGHT, TreeLight.at(LocalTime.of(4, 59), hours));
        assertThrows(IllegalArgumentException.class, () -> new ForestProperties.LightHours(11, 5, 17, 21));
    }
}
