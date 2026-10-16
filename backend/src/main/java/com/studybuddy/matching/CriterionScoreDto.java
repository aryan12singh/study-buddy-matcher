package com.studybuddy.matching;

import com.studybuddy.matching.scoring.MatchingCriterion;

/**
 * The exact criterion compatibility score, whether it influenced this search, and whether
 * both students shared that preference (when not, the score is the configured unknown score).
 */
public record CriterionScoreDto(MatchingCriterion criterion, double score, boolean enabled, boolean known) {
}
