package com.studybuddy.matching.scoring;

import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.student.Student;
import org.springframework.stereotype.Component;

/**
 * Compatible if the two preferred group-size ranges overlap, so "2-3 students"
 * against "2 students" is compatible, as in Appendix A.
 */
@Component
public class GroupSizeScorer implements CriterionScorer {

    @Override
    public MatchingCriterion criterion() {
        return MatchingCriterion.GROUP_SIZE;
    }

    @Override
    public double score(MatchingProfile searcher, MatchingProfile candidate, MatchSearchCriteria search) {
        Student mine = searcher.student();
        Student theirs = candidate.student();
        if (mine.getPreferredGroupSizeMin() == null || mine.getPreferredGroupSizeMax() == null
                || theirs.getPreferredGroupSizeMin() == null || theirs.getPreferredGroupSizeMax() == null) {
            return NEUTRAL_SCORE;
        }
        boolean overlap = mine.getPreferredGroupSizeMin() <= theirs.getPreferredGroupSizeMax()
                && theirs.getPreferredGroupSizeMin() <= mine.getPreferredGroupSizeMax();
        return overlap ? 1.0 : 0.0;
    }
}
