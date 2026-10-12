package com.studybuddy.matching.scoring;

import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.student.StudyGoal;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Scores the share of the searcher's study goals that the candidate also has. */
@Component
public class StudyGoalScorer implements CriterionScorer {

    @Override
    public MatchingCriterion criterion() {
        return MatchingCriterion.STUDY_GOAL;
    }

    @Override
    public double score(MatchingProfile searcher, MatchingProfile candidate, MatchSearchCriteria search) {
        Set<StudyGoal> mine = searcher.student().getStudyGoals();
        Set<StudyGoal> theirs = candidate.student().getStudyGoals();
        if (mine.isEmpty() || theirs.isEmpty()) {
            return NEUTRAL_SCORE;
        }
        long inCommon = mine.stream().filter(theirs::contains).count();
        return (double) inCommon / mine.size();
    }
}
