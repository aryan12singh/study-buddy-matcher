package com.studybuddy.matching.scoring;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.studybuddy.matching.MatchingFixtures.evenWeights;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatchingWeightsTest {

    @Test
    void acceptsWeightsThatAddUpToOne() {
        MatchingWeights weights = new MatchingWeights(evenWeights());

        assertEquals(0.2, weights.weightOf(MatchingCriterion.COURSE));
    }

    @Test
    void allowsRoundingInTypedDecimals() {
        Map<MatchingCriterion, Double> values = evenWeights();
        values.put(MatchingCriterion.COURSE, 0.1 + 0.1 + 0.0001);

        assertEquals(0.2001, new MatchingWeights(values).weightOf(MatchingCriterion.COURSE), 1e-9);
    }

    @Test
    void rejectsWeightsThatDoNotAddUpToOne() {
        Map<MatchingCriterion, Double> tooMuch = evenWeights();
        tooMuch.put(MatchingCriterion.COURSE, 0.5);
        Map<MatchingCriterion, Double> tooLittle = evenWeights();
        tooLittle.put(MatchingCriterion.COURSE, 0.1);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new MatchingWeights(tooMuch));
        assertEquals("The weights must add up to 1, but add up to 1.3", error.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new MatchingWeights(tooLittle));
    }

    @Test
    void zeroIsAllowedForSomeCriteria() {
        Map<MatchingCriterion, Double> values = evenWeights();
        values.put(MatchingCriterion.GROUP_SIZE, 0.0);
        values.put(MatchingCriterion.COURSE, 0.4);

        assertEquals(0.0, new MatchingWeights(values).weightOf(MatchingCriterion.GROUP_SIZE));
    }

    @Test
    void rejectsMissingWeights() {
        assertThrows(IllegalArgumentException.class, () -> new MatchingWeights(null));

        Map<MatchingCriterion, Double> values = evenWeights();
        values.remove(MatchingCriterion.STUDY_GOAL);
        assertThrows(IllegalArgumentException.class, () -> new MatchingWeights(values));
    }

    @Test
    void rejectsNullNegativeAndNonFiniteWeights() {
        Map<MatchingCriterion, Double> withNull = new HashMap<>(evenWeights());
        withNull.put(MatchingCriterion.COURSE, null);
        assertThrows(IllegalArgumentException.class, () -> new MatchingWeights(withNull));

        for (double invalid : new double[] {-0.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            Map<MatchingCriterion, Double> values = evenWeights();
            values.put(MatchingCriterion.COURSE, invalid);
            assertThrows(IllegalArgumentException.class, () -> new MatchingWeights(values));
        }
    }

    @Test
    void rescalingKeepsEachCriterionsShare() {
        Map<MatchingCriterion, Double> saved = evenWeights();
        saved.replaceAll((criterion, weight) -> 1.0);
        saved.put(MatchingCriterion.COURSE, 6.0);

        MatchingWeights weights = MatchingWeights.rescaledToOne(saved);

        assertEquals(0.6, weights.weightOf(MatchingCriterion.COURSE), 1e-9);
        assertEquals(0.1, weights.weightOf(MatchingCriterion.GROUP_SIZE), 1e-9);
    }

    @Test
    void rescalingRoundsToTwoDecimalPlacesAndStillAddsUpToOne() {
        // The relative weights 10, 0.3, 0.2, 2.4 and 0.1 add up to 13.
        Map<MatchingCriterion, Double> saved = Map.of(MatchingCriterion.COURSE, 10.0, MatchingCriterion.AVAILABILITY, 0.3,
                MatchingCriterion.STUDY_MODE, 0.2, MatchingCriterion.STUDY_GOAL, 2.4, MatchingCriterion.GROUP_SIZE, 0.1);

        MatchingWeights weights = MatchingWeights.rescaledToOne(saved);

        assertEquals(0.77, weights.weightOf(MatchingCriterion.COURSE));
        assertEquals(0.02, weights.weightOf(MatchingCriterion.AVAILABILITY));
        assertEquals(0.02, weights.weightOf(MatchingCriterion.STUDY_MODE));
        assertEquals(0.18, weights.weightOf(MatchingCriterion.STUDY_GOAL));
        assertEquals(0.01, weights.weightOf(MatchingCriterion.GROUP_SIZE));
    }

    @Test
    void rescalingRejectsAllZeroWeights() {
        Map<MatchingCriterion, Double> values = evenWeights();
        values.replaceAll((criterion, weight) -> 0.0);

        assertThrows(IllegalArgumentException.class, () -> MatchingWeights.rescaledToOne(values));
    }

    @Test
    void laterChangesToTheSourceMapDoNotAffectTheWeights() {
        Map<MatchingCriterion, Double> values = evenWeights();
        MatchingWeights weights = new MatchingWeights(values);

        values.put(MatchingCriterion.COURSE, 9.0);

        assertEquals(0.2, weights.weightOf(MatchingCriterion.COURSE));
    }
}
