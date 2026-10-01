package com.studybuddy.studygroup;

import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;

import java.util.Set;

/** A study group as it appears in the browse list. */
public record StudyGroupSummaryDto(
        Long id,
        String name,
        Long courseId,
        String courseCode,
        String courseName,
        Long leaderId,
        String leaderName,
        StudyMode preferredStudyMode,
        Set<StudyGoal> studyGoals,
        int maxGroupSize,
        long memberCount,
        boolean active) {
}
