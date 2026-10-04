package com.studybuddy.profile;

import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;

import java.util.List;
import java.util.Set;

/**
 * A profile as the student themselves, or an actively connected student,
 * sees it: every public field plus the contact number.
 */
public record ConnectedProfileDto(
        Long id,
        String name,
        String school,
        String programme,
        Integer yearOfStudy,
        List<ProfileCourseDto> coursesTaken,
        ProfileCourseDto targetCourse,
        StudyMode preferredStudyMode,
        Set<StudyGoal> studyGoals,
        Integer preferredGroupSizeMin,
        Integer preferredGroupSizeMax,
        String contactNumber) implements ProfileDto {
}
