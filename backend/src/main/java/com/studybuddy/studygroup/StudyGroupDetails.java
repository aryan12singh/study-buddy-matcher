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
 * The annotations are for the controller's {@code @Valid}; the service checks
 * the same rules again so it is safe to call without one.
 */
public record StudyGroupDetails(
        @NotBlank String name,
        String description,
        @NotNull Long courseId,
        Set<StudyGoal> studyGoals,
        StudyMode preferredStudyMode,
        @NotNull @Min(StudyGroupDetails.MIN_GROUP_SIZE) Integer maxGroupSize,
        List<@Valid GroupAvailabilitySlotDto> availability) {

    /** A group of one is not a group: the leader plus at least one member. */
    public static final int MIN_GROUP_SIZE = 2;

    /** Missing study goals or availability mean none, not an error. */
    public StudyGroupDetails {
        studyGoals = studyGoals == null ? Set.of() : Set.copyOf(studyGoals);
        availability = availability == null ? List.of() : List.copyOf(availability);
    }
}
