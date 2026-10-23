package com.studybuddy.studyplan;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * One block of a study plan: what to study in a given week before the exam, or in a given meeting
 * of a session. Created only through {@link StudyPlan#addEntry}; the student can only tick it off.
 */
@Entity
@Table(name = "study_plan_entries")
public class StudyPlanEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "plan_id", nullable = false) private StudyPlan plan;
    /** The week number (personal) or meeting number (session), starting at 1. */
    @Column(nullable = false) private int sequence;
    /** The meeting's date for a session plan; null for a personal plan, whose weeks are relative. */
    @Column(name = "scheduled_date") private LocalDate scheduledDate;
    @Column(nullable = false) private int position;
    @Column(nullable = false) private String title;
    @Column(name = "planned_minutes", nullable = false) private int plannedMinutes;
    @Column(nullable = false) private boolean completed;
    @ElementCollection
    @CollectionTable(name = "study_plan_entry_tasks", joinColumns = @JoinColumn(name = "entry_id"))
    @OrderColumn(name = "position")
    @Column(name = "description", nullable = false)
    private List<String> tasks = new ArrayList<>();

    protected StudyPlanEntry() { }

    StudyPlanEntry(StudyPlan plan, int position, int sequence, LocalDate scheduledDate, String title,
            int plannedMinutes, List<String> tasks) {
        if (sequence < 1) throw new IllegalArgumentException("Weeks and meetings are numbered from 1");
        if (plannedMinutes <= 0) throw new IllegalArgumentException("An entry needs planned time");
        this.plan = plan;
        this.position = position;
        this.sequence = sequence;
        this.scheduledDate = scheduledDate;
        this.title = title;
        this.plannedMinutes = plannedMinutes;
        this.tasks = new ArrayList<>(tasks);
    }

    void markCompleted(boolean completed) { this.completed = completed; }

    public Long getId() { return id; }
    public StudyPlan getPlan() { return plan; }
    public int getSequence() { return sequence; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public int getPosition() { return position; }
    public String getTitle() { return title; }
    public int getPlannedMinutes() { return plannedMinutes; }
    public boolean isCompleted() { return completed; }
    public List<String> getTasks() { return List.copyOf(tasks); }
}
