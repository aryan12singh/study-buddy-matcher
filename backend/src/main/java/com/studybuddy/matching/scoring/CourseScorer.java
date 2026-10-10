package com.studybuddy.matching.scoring;

import com.studybuddy.course.Course;
import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.student.Student;
import org.springframework.stereotype.Component;

/**
 * Scores the course in focus: the searched course, or else the searcher's target
 * course. Full marks if the candidate wants a buddy for that course too; partial
 * if they only take it.
 */
@Component
public class CourseScorer implements CriterionScorer {

    /** The candidate takes the course but wants a buddy for a different one. */
    private static final double TAKES_COURSE_SCORE = 0.5;

    @Override
    public MatchingCriterion criterion() {
        return MatchingCriterion.COURSE;
    }

    @Override
    public double score(MatchingProfile searcher, MatchingProfile candidate, MatchSearchCriteria search) {
        Long focus = search.courseId() != null ? search.courseId() : idOf(searcher.student().getTargetCourse());
        if (focus == null) {
            return NEUTRAL_SCORE;
        }
        Student other = candidate.student();
        if (focus.equals(idOf(other.getTargetCourse()))) {
            return 1.0;
        }
        boolean takesCourse = other.getCoursesTaken().stream().anyMatch(course -> focus.equals(course.getId()));
        return takesCourse ? TAKES_COURSE_SCORE : 0.0;
    }

    private static Long idOf(Course course) {
        return course == null ? null : course.getId();
    }
}
