package com.studybuddy.student;

import com.studybuddy.course.CourseDto;
import java.util.List;
import java.util.Set;

/**
 * A student's own profile as they see it when editing. It includes the contact
 * number because the viewer is its owner; other students' profiles are a
 * separate DTO that withholds it until they are connected.
 */
public record MyProfileDto(
        Long id,
        String email,
        String name,
        String school,
        String programme,
        Integer yearOfStudy,
        String contactNumber,
        List<CourseDto> coursesTaken,
        CourseDto targetCourse,
        StudyMode preferredStudyMode,
        GroupSizePreference groupSizePreference,
        int groupSizeMax,
        Set<StudyGoal> studyGoals,
        List<AvailabilitySlotDto> availability) {
}
