package com.studybuddy.studyplan;

import com.studybuddy.course.CourseAssembler;
import org.springframework.stereotype.Component;

/** Builds plan DTOs, so the entity never leaves the service layer. */
@Component
public class StudyPlanAssembler {
    private final CourseAssembler courses;

    public StudyPlanAssembler(CourseAssembler courses) {
        this.courses = courses;
    }

    public StudyPlanDto toDto(StudyPlan plan, boolean canManage) {
        return new StudyPlanDto(plan.getId(), plan.getScope(), courses.toDto(plan.getCourse()),
                plan.getSession() == null ? null : plan.getSession().getId(), plan.getFirstBlockDate(),
                plan.getBlockCount(), plan.getMinutesPerBlock(), plan.getExamDate(), plan.getStudyGoal(),
                plan.getSourceKind(), plan.getSourceFilenames(), plan.getSourceTopics(), canManage,
                plan.getEntries().stream().map(StudyPlanAssembler::toDto).toList(), plan.getCreatedAt(), plan.getUpdatedAt());
    }

    public StudyPlanSummaryDto toSummary(StudyPlan plan) {
        int completed = (int) plan.getEntries().stream().filter(StudyPlanEntry::isCompleted).count();
        return new StudyPlanSummaryDto(plan.getId(), plan.getScope(), courses.toDto(plan.getCourse()),
                plan.getFirstBlockDate(), plan.getBlockCount(), plan.getExamDate(), plan.getStudyGoal(),
                plan.getEntries().size(), completed, plan.getCreatedAt());
    }

    private static StudyPlanEntryDto toDto(StudyPlanEntry entry) {
        return new StudyPlanEntryDto(entry.getId(), entry.getSequence(), entry.getScheduledDate(), entry.getTitle(),
                entry.getPlannedMinutes(), entry.getTasks(), entry.isCompleted());
    }
}
