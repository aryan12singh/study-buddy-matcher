package com.studybuddy.matching.strategy;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import org.springframework.stereotype.Component;

/**
 * Ranks by bands of course alignment first (by default: wants a buddy for the same
 * course, takes it, neither), then by the weighted total.
 */
@Component
public class CourseFirstStrategy extends PriorityTierStrategy {

    public CourseFirstStrategy(PriorityTiers tiers) {
        super(tiers.courseScore());
    }

    @Override
    public MatchingStrategyType type() {
        return MatchingStrategyType.COURSE_FIRST;
    }

    @Override
    protected MatchingCriterion priority() {
        return MatchingCriterion.COURSE;
    }

    @Override
    protected double priorityValue(MatchScore score) {
        return score.scoreFor(MatchingCriterion.COURSE);
    }
}
