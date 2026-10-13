package com.studybuddy.matching;

import com.studybuddy.matching.strategy.MatchingStrategyType;
import java.util.List;

/** A bounded discovery result, identifying the exact scoring configuration used. */
public record MatchSearchResultDto(MatchingStrategyType strategy, long configurationVersion,
        MatchSort sort, int limit, int totalMatches, List<MatchResultDto> matches) {
    public MatchSearchResultDto {
        matches = List.copyOf(matches);
    }
}
