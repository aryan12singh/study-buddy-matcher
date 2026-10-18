package com.studybuddy.matching.scoring;

import com.studybuddy.student.StudyGoal;

import java.util.EnumSet;
import java.util.Set;

/** The saved preferences plus the goal selected for this search, shared by scoring and explanation evidence. */
public final class StudyGoalIntent {
    private StudyGoalIntent() {
    }

    public static Set<StudyGoal> effectiveGoals(Set<StudyGoal> savedGoals, StudyGoal searchedGoal) {
        Set<StudyGoal> goals = EnumSet.noneOf(StudyGoal.class);
        goals.addAll(savedGoals);
        if (searchedGoal != null) {
            goals.add(searchedGoal);
        }
        return Set.copyOf(goals);
    }
}
