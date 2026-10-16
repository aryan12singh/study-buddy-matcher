package com.studybuddy.matching.scoring;

import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.student.StudyGoal;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Scores shared study goals as a share of the smaller goal set (the overlap coefficient).
 * The score is the same from either student's side, and a student open to several goals
 * is treated like one who chose "either" for study mode: compatible, not penalised.
 */
@Component
public class StudyGoalScorer implements CriterionScorer {

    @Override
    public MatchingCriterion criterion() {
        return MatchingCriterion.STUDY_GOAL;
    }

    @Override
    public CriterionResult score(MatchingProfile searcher, MatchingProfile candidate, MatchSearchCriteria search) {
        // The filter already guarantees a searched goal, so the searcher's other saved goals
        // are what tell candidates apart.
        Set<StudyGoal> mine = EnumSet.noneOf(StudyGoal.class);
        mine.addAll(searcher.student().getStudyGoals());
        if (search.studyGoal() != null) {
            mine.add(search.studyGoal());
        }
        Set<StudyGoal> theirs = candidate.student().getStudyGoals();
        if (mine.isEmpty() || theirs.isEmpty()) {
            return CriterionResult.unknown();
        }
        long inCommon = mine.stream().filter(theirs::contains).count();
        return CriterionResult.of((double) inCommon / Math.min(mine.size(), theirs.size()));
    }
}
