package com.studybuddy.matching.strategy;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PriorityTiersTest {

    private static final List<Double> COURSE = List.of(1.0, 0.5);

    @Test
    void keepsBoundariesListedFromHighestToLowest() {
        assertEquals(List.of(3.0, 1.0), new PriorityTiers(List.of(3.0, 1.0), COURSE).availabilityHours());
    }

    @Test
    void rejectsBoundariesThatWouldMakeBandsAmbiguous() {
        for (List<Double> invalid : List.of(List.<Double>of(), List.of(1.0, 3.0), List.of(2.0, 2.0), List.of(1.0, 0.0),
                List.of(1.0, -1.0), List.of(Double.NaN), Arrays.asList(1.0, null))) {
            assertThrows(IllegalArgumentException.class, () -> new PriorityTiers(invalid, COURSE), invalid.toString());
        }
        assertThrows(IllegalArgumentException.class, () -> new PriorityTiers(null, COURSE));
    }
}
