package com.studybuddy.matching.scoring;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatchScoreTest {

    @Test
    void returnsTheScoreForEachCriterion() {
        MatchScore score = new MatchScore(Map.of(MatchingCriterion.COURSE, 1.0, MatchingCriterion.AVAILABILITY, 0.5), 0.75);

        assertEquals(1.0, score.scoreFor(MatchingCriterion.COURSE));
        assertEquals(0.5, score.scoreFor(MatchingCriterion.AVAILABILITY));
        assertEquals(0.75, score.total());
    }

    @Test
    void unscoredCriterionCountsAsZero() {
        MatchScore score = new MatchScore(Map.of(MatchingCriterion.COURSE, 1.0), 1.0);

        assertEquals(0.0, score.scoreFor(MatchingCriterion.GROUP_SIZE));
    }

    @Test
    void laterChangesToTheSourceMapDoNotAffectTheScore() {
        Map<MatchingCriterion, Double> source = new EnumMap<>(MatchingCriterion.class);
        source.put(MatchingCriterion.COURSE, 1.0);
        MatchScore score = new MatchScore(source, 1.0);

        source.put(MatchingCriterion.COURSE, 0.0);

        assertEquals(1.0, score.scoreFor(MatchingCriterion.COURSE));
    }

    @Test
    void breakdownCannotBeModified() {
        MatchScore score = new MatchScore(Map.of(MatchingCriterion.COURSE, 1.0), 1.0);

        assertThrows(UnsupportedOperationException.class,
                () -> score.breakdown().put(MatchingCriterion.COURSE, 0.0));
    }

    @Test
    void rejectsCriterionScoreOutsideZeroToOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new MatchScore(Map.of(MatchingCriterion.COURSE, 1.5), 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> new MatchScore(Map.of(MatchingCriterion.COURSE, -0.1), 0.0));
    }

    @Test
    void rejectsTotalOutsideZeroToOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new MatchScore(Map.of(), 1.2));
    }
}
