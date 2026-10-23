package com.studybuddy.studyplan;

import com.studybuddy.course.Course;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.studysession.StudySession;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A study roadmap: a personal plan paced to one course's exam, or a plan for the meetings of one
 * study session. Keeps the topics extracted from the student's material, never the material itself.
 * The same rules are enforced by the table's check constraints.
 */
@Entity
@Table(name = "study_plans")
public class StudyPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StudyPlanScope scope;
    @ManyToOne @JoinColumn(name = "student_id") private Student student;
    @ManyToOne @JoinColumn(name = "session_id") private StudySession session;
    @Column(name = "created_by") private Long createdBy;
    @ManyToOne(optional = false) @JoinColumn(name = "course_id", nullable = false) private Course course;
    @Column(name = "exam_date") private LocalDate examDate;
    @Column(name = "hours_per_week") private Integer hoursPerWeek;
    @Enumerated(EnumType.STRING) @Column(name = "study_goal", length = 30) private StudyGoal studyGoal;
    @Enumerated(EnumType.STRING) @Column(name = "source_kind", nullable = false, length = 20) private StudyMaterialSource sourceKind;
    @Column(name = "source_filename") private String sourceFilename;
    @ElementCollection
    @CollectionTable(name = "study_plan_source_topics", joinColumns = @JoinColumn(name = "plan_id"))
    @OrderColumn(name = "position")
    @Column(name = "title", nullable = false)
    private List<String> sourceTopics = new ArrayList<>();
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<StudyPlanEntry> entries = new ArrayList<>();
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected StudyPlan() { }

    private StudyPlan(StudyPlanScope scope, Course course, StudyGoal studyGoal, StudyMaterial material,
            List<String> sourceTopics, Long actorId, Instant now) {
        this.scope = scope;
        this.course = Objects.requireNonNull(course, "course");
        this.studyGoal = studyGoal;
        this.sourceKind = material.source();
        this.sourceFilename = material.filename();
        this.sourceTopics = new ArrayList<>(sourceTopics);
        this.createdBy = actorId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** A plan only this student sees, paced week by week to their exam. */
    public static StudyPlan personal(Student student, Course course, LocalDate examDate, int hoursPerWeek,
            StudyGoal studyGoal, StudyMaterial material, List<String> sourceTopics, Instant now) {
        if (examDate == null) throw new IllegalArgumentException("A personal plan needs an exam date");
        if (hoursPerWeek < 1) throw new IllegalArgumentException("A personal plan needs weekly study hours");
        StudyPlan plan = new StudyPlan(StudyPlanScope.PERSONAL, course, studyGoal, material, sourceTopics,
                Objects.requireNonNull(student, "student").getId(), now);
        plan.student = student;
        plan.examDate = examDate;
        plan.hoursPerWeek = hoursPerWeek;
        return plan;
    }

    /** A plan for a session's meetings, managed by whoever may manage the session. */
    public static StudyPlan forSession(StudySession session, Course course, StudyGoal studyGoal,
            StudyMaterial material, List<String> sourceTopics, Long actorId, Instant now) {
        StudyPlan plan = new StudyPlan(StudyPlanScope.SESSION, course, studyGoal, material, sourceTopics, actorId, now);
        plan.session = Objects.requireNonNull(session, "session");
        return plan;
    }

    /** Appends an entry after the existing ones. Only used while the plan is being built. */
    public void addEntry(int sequence, LocalDate scheduledDate, String title, int plannedMinutes, List<String> tasks) {
        entries.add(new StudyPlanEntry(this, entries.size(), sequence, scheduledDate, title, plannedMinutes, tasks));
    }

    /** Ticks an entry off, or un-ticks it. False when the entry is not part of this plan. */
    public boolean markEntry(Long entryId, boolean completed, Instant now) {
        for (StudyPlanEntry entry : entries) {
            if (entry.getId() != null && entry.getId().equals(entryId)) {
                entry.markCompleted(completed);
                updatedAt = now;
                return true;
            }
        }
        return false;
    }

    public Long getId() { return id; }
    public StudyPlanScope getScope() { return scope; }
    public Student getStudent() { return student; }
    public StudySession getSession() { return session; }
    public Long getCreatedBy() { return createdBy; }
    public Course getCourse() { return course; }
    public LocalDate getExamDate() { return examDate; }
    public Integer getHoursPerWeek() { return hoursPerWeek; }
    public StudyGoal getStudyGoal() { return studyGoal; }
    public StudyMaterialSource getSourceKind() { return sourceKind; }
    public String getSourceFilename() { return sourceFilename; }
    public List<String> getSourceTopics() { return List.copyOf(sourceTopics); }
    public List<StudyPlanEntry> getEntries() { return List.copyOf(entries); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
