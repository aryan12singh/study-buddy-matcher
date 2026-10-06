package com.studybuddy.studygroup;

import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What a leader supplies when creating or updating a group. The same shape is
 * used for both, so an update replaces every field, availability included.
 * The annotations are for the controller's {@code @Valid}; the service checks
 * the same rules again so it is safe to call without one.
 */
public record StudyGroupDetails(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 4000) String description,
        @NotNull @Positive Long courseId,
        Set<@NotNull StudyGoal> studyGoals,
        StudyMode preferredStudyMode,
        @NotNull @Min(StudyGroupDetails.MIN_GROUP_SIZE) Integer maxGroupSize,
        List<@NotNull @Valid GroupAvailabilitySlotDto> availability) {
    /** A group of one is not a group: the leader plus at least one member. */
    public static final int MIN_GROUP_SIZE = 2;

    /** Missing study goals or availability mean none, not an error. */
    public StudyGroupDetails {
        studyGoals = studyGoals == null ? Set.of() : Collections.unmodifiableSet(new HashSet<>(studyGoals));
        availability = availability == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(availability));
    }
}
