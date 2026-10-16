package com.studybuddy.matching.scoring;

import com.studybuddy.matching.MatchQuality;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatchScoreTest {

    private static MatchScore score(Map<MatchingCriterion, Double> breakdown, double total) {
        return new MatchScore(breakdown, total, 0.0, MatchQuality.FAIR, Set.of());
    }

    @Test
    void returnsTheScoreForEachCriterion() {
        MatchScore score = score(Map.of(MatchingCriterion.COURSE, 1.0, MatchingCriterion.AVAILABILITY, 0.5), 0.75);

        assertEquals(1.0, score.scoreFor(MatchingCriterion.COURSE));
        assertEquals(0.5, score.scoreFor(MatchingCriterion.AVAILABILITY));
        assertEquals(0.75, score.total());
    }

    @Test
    void unscoredCriterionCountsAsZero() {
        MatchScore score = score(Map.of(MatchingCriterion.COURSE, 1.0), 1.0);

        assertEquals(0.0, score.scoreFor(MatchingCriterion.GROUP_SIZE));
    }

    @Test
    void laterChangesToTheSourceMapDoNotAffectTheScore() {
        Map<MatchingCriterion, Double> source = new EnumMap<>(MatchingCriterion.class);
        source.put(MatchingCriterion.COURSE, 1.0);
        MatchScore score = score(source, 1.0);

        source.put(MatchingCriterion.COURSE, 0.0);

        assertEquals(1.0, score.scoreFor(MatchingCriterion.COURSE));
    }

    @Test
    void breakdownCannotBeModified() {
        MatchScore score = score(Map.of(MatchingCriterion.COURSE, 1.0), 1.0);

        assertThrows(UnsupportedOperationException.class,
                () -> score.breakdown().put(MatchingCriterion.COURSE, 0.0));
    }

    @Test
    void rejectsCriterionScoreOutsideZeroToOne() {
        assertThrows(IllegalArgumentException.class,
                () -> score(Map.of(MatchingCriterion.COURSE, 1.5), 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> score(Map.of(MatchingCriterion.COURSE, -0.1), 0.0));
    }

    @Test
    void rejectsTotalOutsideZeroToOne() {
        assertThrows(IllegalArgumentException.class,
                () -> score(Map.of(), 1.2));
    }
}
