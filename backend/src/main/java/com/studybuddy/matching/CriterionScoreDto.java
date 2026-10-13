package com.studybuddy.matching;

import com.studybuddy.matching.scoring.MatchingCriterion;

/** The exact criterion compatibility score and whether it influenced this search. */
public record CriterionScoreDto(MatchingCriterion criterion, double score, boolean enabled) {
}
