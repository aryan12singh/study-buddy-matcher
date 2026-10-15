package com.studybuddy.matching.strategy;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.scoring.ScoringRules;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ranks by bands of course alignment first, then by the weighted total. The bands are
 * the course scorer's own outcomes, so they cannot drift from it: with a course in
 * focus, wants a buddy for it (1.0), takes it ({@code takes-course-score}), neither.
 * With no course in focus the same boundaries split the share of courses in common:
 * all shared, at least {@code takes-course-score} shared, fewer.
 */
@Component
public class CourseFirstStrategy extends PriorityTierStrategy {

    private static final double FULL_COURSE_MATCH = 1.0;

    public CourseFirstStrategy(ScoringRules rules) {
        super(courseTiers(rules.takesCourseScore()));
    }

    /**
     * Full match, then the takes-course score when it sits strictly between no match and a
     * full match; a takes-course score of 0 or 1 gives "takes it" no band of its own.
     */
    private static List<Double> courseTiers(double takesCourseScore) {
        boolean ownBand = takesCourseScore > 0.0 && takesCourseScore < FULL_COURSE_MATCH;
        return ownBand ? List.of(FULL_COURSE_MATCH, takesCourseScore) : List.of(FULL_COURSE_MATCH);
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
