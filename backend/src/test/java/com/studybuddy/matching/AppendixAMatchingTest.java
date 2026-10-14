package com.studybuddy.matching;

import com.studybuddy.course.Course;
import com.studybuddy.matching.scoring.AvailabilityOverlapScorer;
import com.studybuddy.matching.scoring.CourseScorer;
import com.studybuddy.matching.scoring.GroupSizeScorer;
import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.scoring.MatchingProfile;
import com.studybuddy.matching.scoring.MatchingWeights;
import com.studybuddy.matching.scoring.StudyGoalScorer;
import com.studybuddy.matching.scoring.StudyModeScorer;
import com.studybuddy.matching.scoring.WeightedMatchScorer;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static com.studybuddy.matching.MatchingFixtures.course;
import static com.studybuddy.matching.MatchingFixtures.profile;
import static com.studybuddy.matching.MatchingFixtures.searchForCourse;
import static com.studybuddy.matching.MatchingFixtures.slot;
import static com.studybuddy.matching.MatchingFixtures.student;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reproduces the brief's Appendix A example profile row by row with the real scorers
 * and the default Balanced weights, so the engine is checked against the one worked
 * example the assignment gives.
 */
class AppendixAMatchingTest {

    private static final double TOLERANCE = 1e-9;
    private static final Course IS442 = course(442, "IS442");
    private static final MatchingWeights BALANCED = new MatchingWeights(Map.of(
            MatchingCriterion.COURSE, 0.30, MatchingCriterion.AVAILABILITY, 0.30, MatchingCriterion.STUDY_MODE, 0.20,
            MatchingCriterion.STUDY_GOAL, 0.10, MatchingCriterion.GROUP_SIZE, 0.10));

    private final WeightedMatchScorer scorer = new WeightedMatchScorer(List.of(new CourseScorer(MatchingFixtures.rules()),
            new AvailabilityOverlapScorer(), new StudyModeScorer(), new StudyGoalScorer(), new GroupSizeScorer()),
            new QualityThresholds(0.75, 0.50), MatchingFixtures.rules());

    /** Student A: IS442; Wed 7-9 pm and Fri 2-4 pm; in person; exam preparation; 2-3 students. */
    private static MatchingProfile studentA() {
        Student a = student(1, "Student A");
        a.setTargetCourse(IS442);
        a.getCoursesTaken().add(IS442);
        a.setPreferredStudyMode(StudyMode.IN_PERSON);
        a.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);
        a.setPreferredGroupSizeMin(2);
        a.setPreferredGroupSizeMax(3);
        return profile(a, slot(WEDNESDAY, "19:00", "21:00"), slot(FRIDAY, "14:00", "16:00"));
    }

    /** Student B: IS442; Wed 7-9 pm and Thu 6-8 pm; either mode; exam preparation; 2 students. */
    private static MatchingProfile studentB() {
        Student b = student(2, "Student B");
        b.setTargetCourse(IS442);
        b.getCoursesTaken().add(IS442);
        b.setPreferredStudyMode(StudyMode.EITHER);
        b.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);
        b.setPreferredGroupSizeMin(2);
        b.setPreferredGroupSizeMax(2);
        return profile(b, slot(WEDNESDAY, "19:00", "21:00"), slot(THURSDAY, "18:00", "20:00"));
    }

    private MatchScore score(MatchingProfile searcher, MatchingProfile candidate) {
        return scorer.score(searcher, candidate, searchForCourse(IS442.getId()), BALANCED,
                EnumSet.allOf(MatchingCriterion.class));
    }

    @Test
    void everyRowOfTheExampleTableMatches() {
        MatchScore score = score(studentA(), studentB());

        assertEquals(1.0, score.scoreFor(MatchingCriterion.COURSE), "Course: IS442 / IS442 is a match");
        assertEquals(2.0, score.sharedHoursPerWeek(), TOLERANCE, "Availability: a 2-hour overlap on Wednesday");
        assertEquals(0.5, score.scoreFor(MatchingCriterion.AVAILABILITY), TOLERANCE, "2 of each student's 4 free hours");
        assertEquals(1.0, score.scoreFor(MatchingCriterion.STUDY_MODE), "Study mode: in person / either is compatible");
        assertEquals(1.0, score.scoreFor(MatchingCriterion.STUDY_GOAL), "Study goal: exam preparation on both sides");
        assertEquals(1.0, score.scoreFor(MatchingCriterion.GROUP_SIZE), "Group size: 2-3 / 2 is compatible");
    }

    @Test
    void balancedWeightsMakeTheExampleAStrongMatch() {
        MatchScore score = score(studentA(), studentB());

        // 0.30 x 1 + 0.30 x 0.5 + 0.20 x 1 + 0.10 x 1 + 0.10 x 1
        assertEquals(0.85, score.total(), TOLERANCE);
        assertEquals(MatchQuality.STRONG, score.quality());
    }

    @Test
    void theExampleScoresTheSameFromEitherStudentsSide() {
        assertEquals(score(studentA(), studentB()).total(), score(studentB(), studentA()).total(), TOLERANCE);
    }
}
