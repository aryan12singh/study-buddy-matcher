package com.studybuddy.matching;

import com.studybuddy.course.Course;
import com.studybuddy.matching.availability.TimeSlot;
import com.studybuddy.matching.availability.WeeklyAvailability;
import com.studybuddy.matching.config.MatchingProperties;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.scoring.MatchingProfile;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudyGoal;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Builds students, courses and profiles for matching tests. Ids are assigned by
 * JPA and have no setters, hence reflection.
 */
public final class MatchingFixtures {

    public static final MatchSearchCriteria NO_FILTERS = new MatchSearchCriteria(null, StudyGoal.EXAM_PREPARATION, null, null, null, null);

    private MatchingFixtures() {
    }

    public static Course course(long id, String code) {
        Course course = new Course(code, code + " course");
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }

    public static Student student(long id, String name) {
        Student student = new Student(null, name, "SCIS", "Information Systems", 2, "+65 9000 0000");
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    public static TimeSlot slot(DayOfWeek day, String start, String end) {
        return new TimeSlot(day, LocalTime.parse(start), LocalTime.parse(end));
    }

    public static WeeklyAvailability week(TimeSlot... slots) {
        return new WeeklyAvailability(List.of(slots));
    }

    public static MatchingProfile profile(Student student, TimeSlot... slots) {
        return new MatchingProfile(student, week(slots));
    }

    public static Map<MatchingCriterion, Double> evenWeights() {
        Map<MatchingCriterion, Double> weights = new EnumMap<>(MatchingCriterion.class);
        for (MatchingCriterion criterion : MatchingCriterion.values()) {
            weights.put(criterion, 0.2);
        }
        return weights;
    }

    /** Default weights per strategy, each leaving out the criterion the strategy ranks by first. */
    public static Map<MatchingStrategyType, Map<MatchingCriterion, Double>> strategyDefaults() {
        Map<MatchingCriterion, Double> withoutAvailability = evenWeights();
        withoutAvailability.remove(MatchingCriterion.AVAILABILITY);
        withoutAvailability.replaceAll((criterion, weight) -> 0.25);
        Map<MatchingCriterion, Double> withoutCourse = evenWeights();
        withoutCourse.remove(MatchingCriterion.COURSE);
        withoutCourse.replaceAll((criterion, weight) -> 0.25);
        Map<MatchingStrategyType, Map<MatchingCriterion, Double>> defaults = new EnumMap<>(MatchingStrategyType.class);
        defaults.put(MatchingStrategyType.BALANCED, evenWeights());
        defaults.put(MatchingStrategyType.AVAILABILITY_FIRST, withoutAvailability);
        defaults.put(MatchingStrategyType.COURSE_FIRST, withoutCourse);
        return defaults;
    }

    public static MatchingProperties properties() {
        return new MatchingProperties(MatchingStrategyType.BALANCED, strategyDefaults(), new QualityThresholds(0.75, 0.5));
    }

    public static MatchSearchCriteria searchForCourse(long courseId) {
        return new MatchSearchCriteria(courseId, null, null, null, null, null);
    }
}
