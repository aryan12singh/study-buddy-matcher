package com.studybuddy.matching.scoring;

import com.studybuddy.course.Course;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.studybuddy.matching.scoring.CriterionScorer.NEUTRAL_SCORE;
import static com.studybuddy.matching.MatchingFixtures.NO_FILTERS;
import static com.studybuddy.matching.MatchingFixtures.course;
import static com.studybuddy.matching.MatchingFixtures.profile;
import static com.studybuddy.matching.MatchingFixtures.searchForCourse;
import static com.studybuddy.matching.MatchingFixtures.slot;
import static com.studybuddy.matching.MatchingFixtures.student;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.TUESDAY;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CriterionScorersTest {

    private final Student me = student(1, "Priya");
    private final Student them = student(2, "Jamie");

    @Nested
    class Course_ {
        private final CourseScorer scorer = new CourseScorer();
        private final Course is442 = course(4, "IS442");
        private final Course is216 = course(3, "IS216");

        @Test
        void fullMarksWhenTheCandidateWantsABuddyForTheSearchedCourse() {
            them.setTargetCourse(is442);

            assertEquals(1.0, scorer.score(profile(me), profile(them), searchForCourse(4)));
        }

        @Test
        void partialMarksWhenTheCandidateOnlyTakesIt() {
            them.getCoursesTaken().add(is442);
            them.setTargetCourse(is216);

            assertEquals(0.5, scorer.score(profile(me), profile(them), searchForCourse(4)));
        }

        @Test
        void zeroWhenTheCandidateDoesNotTakeIt() {
            them.getCoursesTaken().add(is216);

            assertEquals(0.0, scorer.score(profile(me), profile(them), searchForCourse(4)));
        }

        @Test
        void usesTheSearchersTargetCourseWhenSearchingByGoal() {
            me.setTargetCourse(is442);
            them.setTargetCourse(is442);

            assertEquals(1.0, scorer.score(profile(me), profile(them), NO_FILTERS));
        }

        @Test
        void neutralWithNoSearchedOrTargetCourse() {
            assertEquals(NEUTRAL_SCORE, scorer.score(profile(me), profile(them), NO_FILTERS));
        }
    }

    @Nested
    class Availability {
        private final AvailabilityOverlapScorer scorer = new AvailabilityOverlapScorer();

        @Test
        void scoresTheShareOfMyFreeTimeTheyShare() {
            MatchingProfile mine = profile(me, slot(MONDAY, "09:00", "11:00"), slot(TUESDAY, "09:00", "11:00"));
            MatchingProfile theirs = profile(them, slot(MONDAY, "10:00", "13:00"));

            assertEquals(0.25, scorer.score(mine, theirs, NO_FILTERS));
        }

        @Test
        void fullMarksWhenAllMyFreeTimeIsShared() {
            assertEquals(1.0, scorer.score(profile(me, slot(MONDAY, "09:00", "10:00")),
                    profile(them, slot(MONDAY, "08:00", "12:00")), NO_FILTERS));
        }

        @Test
        void zeroWhenNoTimeIsShared() {
            assertEquals(0.0, scorer.score(profile(me, slot(MONDAY, "09:00", "10:00")),
                    profile(them, slot(TUESDAY, "09:00", "10:00")), NO_FILTERS));
        }

        @Test
        void neutralWhenEitherStudentHasNoAvailability() {
            assertEquals(NEUTRAL_SCORE, scorer.score(profile(me), profile(them, slot(MONDAY, "09:00", "10:00")), NO_FILTERS));
            assertEquals(NEUTRAL_SCORE, scorer.score(profile(me, slot(MONDAY, "09:00", "10:00")), profile(them), NO_FILTERS));
        }
    }

    @Nested
    class StudyMode_ {
        private final StudyModeScorer scorer = new StudyModeScorer();

        private double score(StudyMode mine, StudyMode theirs) {
            me.setPreferredStudyMode(mine);
            them.setPreferredStudyMode(theirs);
            return scorer.score(profile(me), profile(them), NO_FILTERS);
        }

        @Test
        void compatibleWhenTheSameOrEitherIsFlexible() {
            assertEquals(1.0, score(StudyMode.ONLINE, StudyMode.ONLINE));
            assertEquals(1.0, score(StudyMode.EITHER, StudyMode.IN_PERSON));
            assertEquals(1.0, score(StudyMode.ONLINE, StudyMode.EITHER));
        }

        @Test
        void incompatibleWhenOneIsInPersonAndTheOtherOnline() {
            assertEquals(0.0, score(StudyMode.IN_PERSON, StudyMode.ONLINE));
        }

        @Test
        void neutralWhenEitherHasNoPreference() {
            assertEquals(NEUTRAL_SCORE, score(null, StudyMode.ONLINE));
        }
    }

    @Nested
    class StudyGoals {
        private final StudyGoalScorer scorer = new StudyGoalScorer();

        @Test
        void scoresTheShareOfMyGoalsTheyShare() {
            me.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);
            me.getStudyGoals().add(StudyGoal.CONCEPT_REVIEW);
            them.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);
            them.getStudyGoals().add(StudyGoal.PROJECT_DISCUSSION);

            assertEquals(0.5, scorer.score(profile(me), profile(them), NO_FILTERS));
        }

        @Test
        void zeroWithNoGoalsInCommon() {
            me.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);
            them.getStudyGoals().add(StudyGoal.PROBLEM_SOLVING);

            assertEquals(0.0, scorer.score(profile(me), profile(them), NO_FILTERS));
        }

        @Test
        void neutralWhenEitherHasNoGoals() {
            me.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);

            assertEquals(NEUTRAL_SCORE, scorer.score(profile(me), profile(them), NO_FILTERS));
        }
    }

    @Nested
    class GroupSize {
        private final GroupSizeScorer scorer = new GroupSizeScorer();

        private double score(int myMin, int myMax, int theirMin, int theirMax) {
            me.setPreferredGroupSizeMin(myMin);
            me.setPreferredGroupSizeMax(myMax);
            them.setPreferredGroupSizeMin(theirMin);
            them.setPreferredGroupSizeMax(theirMax);
            return scorer.score(profile(me), profile(them), NO_FILTERS);
        }

        @Test
        void compatibleWhenTheRangesOverlap() {
            // Appendix A: "2-3 students" against "2 students" is compatible.
            assertEquals(1.0, score(2, 3, 2, 2));
            assertEquals(1.0, score(3, 5, 2, 3));
        }

        @Test
        void incompatibleWhenTheRangesAreApart() {
            assertEquals(0.0, score(2, 2, 3, 5));
        }

        @Test
        void neutralWhenEitherHasNoPreference() {
            me.setPreferredGroupSizeMin(2);
            me.setPreferredGroupSizeMax(3);

            assertEquals(NEUTRAL_SCORE, scorer.score(profile(me), profile(them), NO_FILTERS));
        }
    }
}
