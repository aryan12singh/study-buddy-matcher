package com.studybuddy.matching.strategy;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Set;

/** Ranks by course match first, then by the configured weighted total. */
@Component
public class CourseFirstStrategy implements MatchingStrategy {

    @Override
    public MatchingStrategyType type() {
        return MatchingStrategyType.COURSE_FIRST;
    }

    @Override
    public Comparator<MatchScore> ranking() {
        return Comparator.comparingDouble((MatchScore score) -> score.scoreFor(MatchingCriterion.COURSE))
                .thenComparingDouble(MatchScore::total)
                .reversed();
    }

    @Override
    public Comparator<MatchScore> ranking(Set<MatchingCriterion> enabled) {
        return enabled.contains(MatchingCriterion.COURSE) ? ranking()
                : Comparator.comparingDouble(MatchScore::total).reversed();
    }
}
