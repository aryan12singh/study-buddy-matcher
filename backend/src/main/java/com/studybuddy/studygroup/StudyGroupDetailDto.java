package com.studybuddy.studygroup;

import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/** A study group's full detail page: summary fields plus availability and members. */
public record StudyGroupDetailDto(
        Long id,
        String name,
        String description,
        Long courseId,
        String courseCode,
        String courseName,
        Long leaderId,
        String leaderName,
        StudyMode preferredStudyMode,
        Set<StudyGoal> studyGoals,
        int maxGroupSize,
        long memberCount,
        boolean active,
        Instant createdAt,
        List<GroupAvailabilitySlotDto> availability,
        List<GroupMemberDto> members,
        GroupViewerDto viewer,
        /** Applications waiting for a decision; only the leader receives a number, everyone else null. */
        Long pendingApplications) {
}
