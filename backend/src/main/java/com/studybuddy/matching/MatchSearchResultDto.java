package com.studybuddy.matching;

import com.studybuddy.matching.strategy.MatchingStrategyType;

import java.util.List;

/**
 * A match search's results with the admin's strategy that ranked them, so the
 * student can see how the order was decided.
 */
public record MatchSearchResultDto(MatchingStrategyType strategy, List<MatchResultDto> matches) {
}
