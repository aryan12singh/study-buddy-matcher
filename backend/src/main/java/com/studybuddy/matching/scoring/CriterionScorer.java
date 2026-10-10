package com.studybuddy.matching.scoring;

import com.studybuddy.matching.MatchSearchCriteria;
/**
 * Scores how compatible two students are on a single criterion. Each criterion
 * has its own implementation, so adding one never touches the others.
 */
public interface CriterionScorer {

    /**
     * The score when either student has not filled in this preference, so an
     * incomplete profile is neither rewarded nor punished.
     */
    double NEUTRAL_SCORE = 0.5;

    MatchingCriterion criterion();

    /** Returns a score from 0.0 (no compatibility) to 1.0 (fully compatible). */
    double score(MatchingProfile searcher, MatchingProfile candidate, MatchSearchCriteria search);
}
