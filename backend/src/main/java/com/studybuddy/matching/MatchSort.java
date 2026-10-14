package com.studybuddy.matching;

import com.studybuddy.matching.scoring.MatchingProfile;

import java.util.Comparator;
import java.util.Objects;

/**
 * User-selected ordering. Each option says what it compares before the
 * administrator's active strategy; best match compares nothing first, so the
 * strategy decides.
 */
public enum MatchSort {
    BEST_MATCH {
        @Override
        Comparator<ScoredCandidate> before(MatchingProfile searcher) {
            return (first, second) -> 0;
        }
    },
    MOST_SHARED_HOURS {
        @Override
        Comparator<ScoredCandidate> before(MatchingProfile searcher) {
            return Comparator.comparingDouble((ScoredCandidate candidate) -> candidate.score().sharedHoursPerWeek()).reversed();
        }
    },
    SAME_YEAR_FIRST {
        @Override
        Comparator<ScoredCandidate> before(MatchingProfile searcher) {
            Integer myYear = searcher.student().getYearOfStudy();
            return Comparator.comparing((ScoredCandidate candidate) -> myYear != null
                    && Objects.equals(myYear, candidate.profile().student().getYearOfStudy())).reversed();
        }
    };

    /** What this option compares before the strategy's ranking. */
    abstract Comparator<ScoredCandidate> before(MatchingProfile searcher);
}
