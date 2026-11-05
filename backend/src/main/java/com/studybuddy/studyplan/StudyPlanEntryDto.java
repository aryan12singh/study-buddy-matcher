package com.studybuddy.studyplan;

import java.time.LocalDate;
import java.util.List;

/**
 * One dated block of a study plan: its title, planned minutes, tasks and whether the student has
 * marked it done.
 */
public record StudyPlanEntryDto(
        Long id,
        int sequence,
        LocalDate scheduledDate,
        String title,
        int plannedMinutes,
        List<String> tasks,
        boolean completed) {
}
