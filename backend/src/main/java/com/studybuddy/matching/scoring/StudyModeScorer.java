package com.studybuddy.matching.scoring;

import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.student.StudyMode;
import org.springframework.stereotype.Component;

/** Compatible if both prefer the same mode or either is happy with both. */
@Component
public class StudyModeScorer implements CriterionScorer {

    @Override
    public MatchingCriterion criterion() {
        return MatchingCriterion.STUDY_MODE;
    }

    @Override
    public double score(MatchingProfile searcher, MatchingProfile candidate, MatchSearchCriteria search) {
        StudyMode mine = searcher.student().getPreferredStudyMode();
        StudyMode theirs = candidate.student().getPreferredStudyMode();
        if (mine == null || theirs == null) {
            return NEUTRAL_SCORE;
        }
        return mine == theirs || mine == StudyMode.EITHER || theirs == StudyMode.EITHER ? 1.0 : 0.0;
    }
}
