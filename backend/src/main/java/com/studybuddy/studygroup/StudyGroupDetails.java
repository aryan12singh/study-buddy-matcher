package com.studybuddy.studygroup;

import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

/**
 * What a leader supplies when creating or updating a group. The same shape is
 * used for both, so an update replaces every field, availability included.
 */
public record StudyGroupDetails(
        @NotBlank String name,
        String description,
        @NotNull Long courseId,
        @NotNull Set<StudyGoal> studyGoals,
        StudyMode preferredStudyMode,
        @NotNull @Min(StudyGroupDetails.MIN_GROUP_SIZE) Integer maxGroupSize,
        @NotNull List<@Valid GroupAvailabilitySlotDto> availability) {

    /** A group of one is not a group: the leader plus at least one member. */
    public static final int MIN_GROUP_SIZE = 2;
}
