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
 * A study roadmap covering one or more consecutive weeks of a student's own study, or one or more
 * upcoming meetings of a study session. Built in one go from a validated {@link RoadmapDraft}, so
 * every entry has a block of the plan and that block's date. Keeps the topics extracted from the
 * student's material and the files' names, never the material itself. The same rules are
 * enforced by the table's check constraints.
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
    @Column(name = "first_block_date", nullable = false) private LocalDate firstBlockDate;
    @Column(name = "block_count", nullable = false) private int blockCount;
    @Column(name = "minutes_per_block", nullable = false) private int minutesPerBlock;
    @Column(name = "exam_date") private LocalDate examDate;
    @Enumerated(EnumType.STRING) @Column(name = "study_goal", length = 30) private StudyGoal studyGoal;
    @Enumerated(EnumType.STRING) @Column(name = "source_kind", nullable = false, length = 20) private StudyMaterialSource sourceKind;
    @ElementCollection
    @CollectionTable(name = "study_plan_source_files", joinColumns = @JoinColumn(name = "plan_id"))
    @OrderColumn(name = "position")
    @Column(name = "filename", nullable = false)
    private List<String> sourceFilenames = new ArrayList<>();
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

    private StudyPlan(StudyPlanScope scope, Course course, StudyGoal studyGoal, RoadmapFrame frame,
            StudyMaterial material, RoadmapDraft draft, Long actorId, Instant now) {
        this.scope = scope;
        this.course = Objects.requireNonNull(course, "course");
        this.studyGoal = studyGoal;
        this.firstBlockDate = frame.firstDate();
        this.blockCount = frame.blocks().size();
        this.minutesPerBlock = frame.minutesPerBlock();
        this.sourceKind = material.source();
        this.sourceFilenames = new ArrayList<>(material.filenames());
        this.sourceTopics = new ArrayList<>(draft.sourceTopics());
        this.createdBy = actorId;
        this.createdAt = now;
        this.updatedAt = now;
        for (RoadmapDraft.Entry entry : draft.entries()) {
            RoadmapBlock block = frame.block(entry.sequence())
                    .orElseThrow(() -> new IllegalArgumentException("An entry names a block outside the plan"));
            entries.add(new StudyPlanEntry(this, entries.size(), entry.sequence(), block.date(), entry.title(),
                    entry.plannedMinutes(), entry.tasks()));
        }
    }

    /**
     * A plan only this student sees, for one or more weeks of their own study.
     *
     * @param examDate the exam the weeks lead up to, or null for a plan of the coming week(s) alone
     */
    public static StudyPlan personal(Student student, Course course, StudyGoal studyGoal, LocalDate examDate,
            RoadmapFrame frame, StudyMaterial material, RoadmapDraft draft, Instant now) {
        if (frame.kind() != RoadmapFrame.BlockKind.WEEK) throw new IllegalArgumentException("A personal plan is planned in weeks");
        if (examDate != null && examDate.isBefore(frame.firstDate())) throw new IllegalArgumentException("The exam cannot be before the plan starts");
        StudyPlan plan = new StudyPlan(StudyPlanScope.PERSONAL, course, studyGoal, frame, material, draft,
                Objects.requireNonNull(student, "student").getId(), now);
        plan.student = student;
        plan.examDate = examDate;
        return plan;
    }

    /** A plan for one or more of a session's upcoming meetings, managed by whoever may manage the session. */
    public static StudyPlan forSession(StudySession session, Course course, StudyGoal studyGoal, RoadmapFrame frame,
            StudyMaterial material, RoadmapDraft draft, Long actorId, Instant now) {
        if (frame.kind() != RoadmapFrame.BlockKind.MEETING) throw new IllegalArgumentException("A session plan is planned in meetings");
        StudyPlan plan = new StudyPlan(StudyPlanScope.SESSION, course, studyGoal, frame, material, draft, actorId, now);
        plan.session = Objects.requireNonNull(session, "session");
        return plan;
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
    public LocalDate getFirstBlockDate() { return firstBlockDate; }
    public int getBlockCount() { return blockCount; }
    public int getMinutesPerBlock() { return minutesPerBlock; }
    public LocalDate getExamDate() { return examDate; }
    public StudyGoal getStudyGoal() { return studyGoal; }
    public StudyMaterialSource getSourceKind() { return sourceKind; }
    public List<String> getSourceFilenames() { return List.copyOf(sourceFilenames); }
    public List<String> getSourceTopics() { return List.copyOf(sourceTopics); }
    public List<StudyPlanEntry> getEntries() { return List.copyOf(entries); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
