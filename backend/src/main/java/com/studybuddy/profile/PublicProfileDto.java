package com.studybuddy.profile;

import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import java.util.List;
import java.util.Set;

/**
 * A profile as any student sees it before they are connected. Has no contact
 * number field at all, so there is nothing to leak: hiding it in the React
 * layer would still send it over the network.
 *
 * <p>Preferred group size is a min and max because that is how
 * {@code Student} stores it; see the open question on group size in AGENTS.md.
 */
public record PublicProfileDto(
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
        List<ProfileAvailabilitySlotDto> availability,
        ProfileRelationshipDto relationship) implements ProfileDto {
}
