package com.studybuddy.matching;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QualityThresholdsTest {

    private final QualityThresholds thresholds = new QualityThresholds(0.75, 0.5);

    @Test
    void classifiesAtAndAroundTheCutOffs() {
        assertEquals(MatchQuality.STRONG, thresholds.classify(0.75));
        assertEquals(MatchQuality.GOOD, thresholds.classify(0.74));
        assertEquals(MatchQuality.GOOD, thresholds.classify(0.5));
        assertEquals(MatchQuality.FAIR, thresholds.classify(0.49));
    }

    @Test
    void rejectsCutOffsOutOfOrderOrRange() {
        assertThrows(IllegalArgumentException.class, () -> new QualityThresholds(0.4, 0.6));
        assertThrows(IllegalArgumentException.class, () -> new QualityThresholds(1.2, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new QualityThresholds(0.75, -0.1));
    }

    @Test
    void qualityComparesBestFirst() {
        assertTrue(MatchQuality.STRONG.isAtLeast(MatchQuality.GOOD));
        assertTrue(MatchQuality.GOOD.isAtLeast(MatchQuality.GOOD));
        assertFalse(MatchQuality.FAIR.isAtLeast(MatchQuality.GOOD));
    }
}
