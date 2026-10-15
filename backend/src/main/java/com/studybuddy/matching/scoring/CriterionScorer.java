package com.studybuddy.matching.scoring;

import com.studybuddy.matching.MatchSearchCriteria;

/**
 * Scores how compatible two students are on a single criterion. Each criterion
 * has its own implementation, so adding one never touches the others.
 */
public interface CriterionScorer {

    MatchingCriterion criterion();

    /**
     * Returns a known score from 0.0 (no compatibility) to 1.0 (fully compatible), or
     * {@link CriterionResult#unknown()} when either student has not filled in this
     * preference. Scorers never invent a number for missing data.
     */
    CriterionResult score(MatchingProfile searcher, MatchingProfile candidate, MatchSearchCriteria search);
}
