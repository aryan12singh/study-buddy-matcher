package com.studybuddy.matching.scoring;

import com.studybuddy.course.Course;
import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.student.Student;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Scores course alignment. With a course in focus (the searched course, or else the
 * searcher's target course): full marks if the candidate wants a buddy for that
 * course too, the configured partial score if they only take it. With no course in
 * focus, scores the courses the two students share, as a share of the smaller
 * course list, so students sharing more of their courses rank higher.
 */
@Component
public class CourseScorer implements CriterionScorer {

    private final ScoringRules rules;

    public CourseScorer(ScoringRules rules) {
        this.rules = rules;
    }

    @Override
    public MatchingCriterion criterion() {
        return MatchingCriterion.COURSE;
    }

    @Override
    public CriterionResult score(MatchingProfile searcher, MatchingProfile candidate, MatchSearchCriteria search) {
        Long focus = search.courseId() != null ? search.courseId() : idOf(searcher.student().getTargetCourse());
        Student other = candidate.student();
        if (focus == null) {
            return sharedCourses(searcher.student(), other);
        }
        if (focus.equals(idOf(other.getTargetCourse()))) {
            return CriterionResult.of(1.0);
        }
        boolean takesCourse = other.getCoursesTaken().stream().anyMatch(course -> focus.equals(course.getId()));
        return CriterionResult.of(takesCourse ? rules.takesCourseScore() : 0.0);
    }

    private static CriterionResult sharedCourses(Student mine, Student theirs) {
        Set<Long> myCourses = currentCourseIds(mine);
        Set<Long> theirCourses = currentCourseIds(theirs);
        if (myCourses.isEmpty() || theirCourses.isEmpty()) {
            return CriterionResult.unknown();
        }
        Set<Long> shared = new HashSet<>(myCourses);
        shared.retainAll(theirCourses);
        return CriterionResult.of((double) shared.size() / Math.min(myCourses.size(), theirCourses.size()));
    }

    /** The courses a student takes, plus the course they want a buddy for. */
    private static Set<Long> currentCourseIds(Student student) {
        return Stream.concat(student.getCoursesTaken().stream(), Stream.ofNullable(student.getTargetCourse()))
                .map(Course::getId)
                .collect(Collectors.toSet());
    }

    private static Long idOf(Course course) {
        return course == null ? null : course.getId();
    }
}
