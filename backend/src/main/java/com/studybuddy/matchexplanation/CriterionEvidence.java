package com.studybuddy.matchexplanation;

import com.studybuddy.matching.scoring.MatchingCriterion;

/**
 * One criterion's rounded score and weight (how much it counts towards the total), whether the admin's settings let it count, and whether both
 * students shared that preference. An unshared one scores the configured unknown score, which
 * says nothing about how well the two students fit, so it must not be worded as a mismatch.
 */
public record CriterionEvidence(MatchingCriterion criterion, double score, double weight, boolean enabled, boolean known) {
}
