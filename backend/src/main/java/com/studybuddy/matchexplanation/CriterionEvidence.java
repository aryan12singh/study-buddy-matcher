package com.studybuddy.matchexplanation;

import com.studybuddy.matching.scoring.MatchingCriterion;

/** One criterion's rounded score, and whether the admin's settings let it count. */
public record CriterionEvidence(MatchingCriterion criterion, double score, boolean enabled) {
}
