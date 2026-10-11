package com.studybuddy.matching;

import com.studybuddy.matching.scoring.MatchingCriterion;

/**
 * How well two students fit on one criterion, from 0.0 to 1.0, rounded to two
 * places. Drives the score breakdown bars; the weighted total stays hidden.
 */
public record CriterionScoreDto(MatchingCriterion criterion, double score) {
}
