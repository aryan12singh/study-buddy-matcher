package com.studybuddy.studyplan;

import java.time.LocalDate;
import java.util.List;

public record StudyPlanEntryDto(
        Long id,
        int sequence,
        LocalDate scheduledDate,
        String title,
        int plannedMinutes,
        List<String> tasks,
        boolean completed) {
}
