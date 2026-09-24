package com.studybuddy.student;

import com.studybuddy.course.Course;
import com.studybuddy.user.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * A student's profile and study preferences. Shares its primary key with the
 * owning {@link User} row (composition, not inheritance) so an admin account
 * never needs to carry these fields.
 */
@Entity
@Table(name = "students")
public class Student {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String school;

    @Column(nullable = false)
    private String programme;

    @Column(name = "year_of_study", nullable = false)
    private Integer yearOfStudy;

    @Column(name = "contact_number", nullable = false)
    private String contactNumber;

    @ManyToMany
    @JoinTable(
            name = "student_courses",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> coursesTaken = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "target_course_id")
    private Course targetCourse;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_study_mode", length = 20)
    private StudyMode preferredStudyMode;

    // Preferred group size is stored as a numeric interval (e.g. min=2, max=3)
    // rather than the categorical one-to-one/small-group/either reading, to
    // match Appendix A's "2-3 students" vs "2 students" -> Compatible example.
    // Open question for Team A per AGENTS.md; swap for an enum if they decide
    // the categorical reading instead.
    @Column(name = "preferred_group_size_min")
    private Integer preferredGroupSizeMin;

    @Column(name = "preferred_group_size_max")
    private Integer preferredGroupSizeMax;

    @ElementCollection(targetClass = StudyGoal.class)
    @CollectionTable(name = "student_study_goals", joinColumns = @JoinColumn(name = "student_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "study_goal", length = 30)
    private Set<StudyGoal> studyGoals = new HashSet<>();

    protected Student() {
    }

    public Student(User user, String name, String school, String programme,
                    Integer yearOfStudy, String contactNumber) {
        this.user = user;
        this.name = name;
        this.school = school;
        this.programme = programme;
        this.yearOfStudy = yearOfStudy;
        this.contactNumber = contactNumber;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getProgramme() {
        return programme;
    }

    public void setProgramme(String programme) {
        this.programme = programme;
    }

    public Integer getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(Integer yearOfStudy) {
        this.yearOfStudy = yearOfStudy;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public Set<Course> getCoursesTaken() {
        return coursesTaken;
    }

    public Course getTargetCourse() {
        return targetCourse;
    }

    public void setTargetCourse(Course targetCourse) {
        this.targetCourse = targetCourse;
    }

    public StudyMode getPreferredStudyMode() {
        return preferredStudyMode;
    }

    public void setPreferredStudyMode(StudyMode preferredStudyMode) {
        this.preferredStudyMode = preferredStudyMode;
    }

    public Integer getPreferredGroupSizeMin() {
        return preferredGroupSizeMin;
    }

    public void setPreferredGroupSizeMin(Integer preferredGroupSizeMin) {
        this.preferredGroupSizeMin = preferredGroupSizeMin;
    }

    public Integer getPreferredGroupSizeMax() {
        return preferredGroupSizeMax;
    }

    public void setPreferredGroupSizeMax(Integer preferredGroupSizeMax) {
        this.preferredGroupSizeMax = preferredGroupSizeMax;
    }

    public Set<StudyGoal> getStudyGoals() {
        return studyGoals;
    }
}
