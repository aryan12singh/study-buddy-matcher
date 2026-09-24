package com.studybuddy.studygroup;

import com.studybuddy.course.Course;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
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
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "study_groups")
public class StudyGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne
    @JoinColumn(name = "leader_id", nullable = false)
    private Student leader;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_study_mode", length = 20)
    private StudyMode preferredStudyMode;

    @Column(name = "max_group_size", nullable = false)
    private Integer maxGroupSize;

    @ElementCollection(targetClass = StudyGoal.class)
    @CollectionTable(name = "study_group_goals", joinColumns = @JoinColumn(name = "study_group_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "study_goal", length = 30)
    private Set<StudyGoal> studyGoals = new HashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected StudyGroup() {
    }

    public StudyGroup(String name, String description, Course course, Student leader, Integer maxGroupSize) {
        this.name = name;
        this.description = description;
        this.course = course;
        this.leader = leader;
        this.maxGroupSize = maxGroupSize;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Course getCourse() {
        return course;
    }

    public Student getLeader() {
        return leader;
    }

    public StudyMode getPreferredStudyMode() {
        return preferredStudyMode;
    }

    public void setPreferredStudyMode(StudyMode preferredStudyMode) {
        this.preferredStudyMode = preferredStudyMode;
    }

    public Integer getMaxGroupSize() {
        return maxGroupSize;
    }

    public void setMaxGroupSize(Integer maxGroupSize) {
        this.maxGroupSize = maxGroupSize;
    }

    public Set<StudyGoal> getStudyGoals() {
        return studyGoals;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
