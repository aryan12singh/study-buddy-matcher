package com.studybuddy.matching.scoring;

import com.studybuddy.course.Course;
import com.studybuddy.matching.MatchingFixtures;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.studybuddy.matching.MatchingFixtures.NO_FILTERS;
import static com.studybuddy.matching.MatchingFixtures.course;
import static com.studybuddy.matching.MatchingFixtures.profile;
import static com.studybuddy.matching.MatchingFixtures.searchForCourse;
import static com.studybuddy.matching.MatchingFixtures.slot;
import static com.studybuddy.matching.MatchingFixtures.student;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CriterionScorersTest {

    private final Student me = student(1, "Priya");
    private final Student them = student(2, "Jamie");

    @Nested
    class Course_ {
        private final CourseScorer scorer = new CourseScorer(MatchingFixtures.rules());
        private final Course is442 = course(4, "IS442");
        private final Course is216 = course(3, "IS216");

        @Test
        void fullMarksWhenTheCandidateWantsABuddyForTheSearchedCourse() {
            them.setTargetCourse(is442);

            assertEquals(1.0, scorer.score(profile(me), profile(them), searchForCourse(4)).score());
        }

        @Test
        void partialMarksWhenTheCandidateOnlyTakesIt() {
            them.getCoursesTaken().add(is442);
            them.setTargetCourse(is216);

            assertEquals(0.5, scorer.score(profile(me), profile(them), searchForCourse(4)).score());
        }

        @Test
        void zeroWhenTheCandidateDoesNotTakeIt() {
            them.getCoursesTaken().add(is216);

            assertEquals(0.0, scorer.score(profile(me), profile(them), searchForCourse(4)).score());
        }

        @Test
        void usesTheSearchersTargetCourseWhenSearchingByGoal() {
            me.setTargetCourse(is442);
            them.setTargetCourse(is442);

            assertEquals(1.0, scorer.score(profile(me), profile(them), NO_FILTERS).score());
        }

        @Test
        void unknownWithNoSearchedOrTargetCourseAndNoCourses() {
            assertEquals(CriterionResult.unknown(), scorer.score(profile(me), profile(them), NO_FILTERS));
        }

        @Test
        void scoresSharedCoursesWhenNoCourseIsInFocus() {
            Course is212 = course(2, "IS212");
            me.getCoursesTaken().addAll(List.of(is442, is216, is212));
            them.getCoursesTaken().addAll(List.of(is442, is216));
            Student fewerShared = student(3, "Alex");
            fewerShared.getCoursesTaken().addAll(List.of(is442, course(5, "IS111")));

            assertEquals(1.0, scorer.score(profile(me), profile(them), NO_FILTERS).score(), "both of their courses are mine");
            assertEquals(0.5, scorer.score(profile(me), profile(fewerShared), NO_FILTERS).score(), "one of their two");
        }

        @Test
        void partialMarksComeFromConfiguration() {
            them.getCoursesTaken().add(is442);
            CourseScorer generous = new CourseScorer(new ScoringRules(0.0, 0.7, 60));

            assertEquals(0.7, generous.score(profile(me), profile(them), searchForCourse(4)).score());
        }
    }

    @Nested
    class Availability {
        private final AvailabilityOverlapScorer scorer = new AvailabilityOverlapScorer(MatchingFixtures.rules());

        @Test
        void scoresSharedTimeAgainstTheSmallerTimetable() {
            // 1 shared hour; my week has 4 free hours and theirs 3, so 1 of 3.
            MatchingProfile mine = profile(me, slot(MONDAY, "09:00", "11:00"), slot(TUESDAY, "09:00", "11:00"));
            MatchingProfile theirs = profile(them, slot(MONDAY, "10:00", "13:00"));

            assertEquals(1.0 / 3, scorer.score(mine, theirs, NO_FILTERS).score(), 1e-9);
        }

        @Test
        void scoresTheSameFromEitherStudentsSide() {
            MatchingProfile small = profile(me, slot(MONDAY, "09:00", "11:00"));
            MatchingProfile large = profile(them, slot(MONDAY, "09:00", "11:00"), slot(TUESDAY, "09:00", "17:00"),
                    slot(WEDNESDAY, "09:00", "17:00"));

            assertEquals(scorer.score(small, large, NO_FILTERS), scorer.score(large, small, NO_FILTERS));
            assertEquals(1.0, scorer.score(large, small, NO_FILTERS).score(), "a busy timetable is not penalised");
        }

        @Test
        void fullMarksWhenAllMyFreeTimeIsShared() {
            assertEquals(1.0, scorer.score(profile(me, slot(MONDAY, "09:00", "10:00")),
                    profile(them, slot(MONDAY, "08:00", "12:00")), NO_FILTERS).score());
        }

        @Test
        void zeroWhenNoTimeIsShared() {
            assertEquals(0.0, scorer.score(profile(me, slot(MONDAY, "09:00", "10:00")),
                    profile(them, slot(TUESDAY, "09:00", "10:00")), NO_FILTERS).score());
        }

        @Test
        void unknownWhenEitherStudentHasNoAvailability() {
            assertEquals(CriterionResult.unknown(), scorer.score(profile(me), profile(them, slot(MONDAY, "09:00", "10:00")), NO_FILTERS));
            assertEquals(CriterionResult.unknown(), scorer.score(profile(me, slot(MONDAY, "09:00", "10:00")), profile(them), NO_FILTERS));
        }
    }

    @Nested
    class StudyMode_ {
        private final StudyModeScorer scorer = new StudyModeScorer();

        private double score(StudyMode mine, StudyMode theirs) {
            me.setPreferredStudyMode(mine);
            them.setPreferredStudyMode(theirs);
            return scorer.score(profile(me), profile(them), NO_FILTERS).score();
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
        void unknownWhenEitherHasNoPreference() {
            me.setPreferredStudyMode(null);
            them.setPreferredStudyMode(StudyMode.ONLINE);
            assertEquals(CriterionResult.unknown(), scorer.score(profile(me), profile(them), NO_FILTERS));
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

            assertEquals(0.5, scorer.score(profile(me), profile(them), NO_FILTERS).score());
        }

        @Test
        void scoresTheSameFromEitherStudentsSide() {
            me.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);
            them.getStudyGoals().addAll(List.of(StudyGoal.EXAM_PREPARATION, StudyGoal.PROJECT_DISCUSSION, StudyGoal.CONCEPT_REVIEW));

            assertEquals(1.0, scorer.score(profile(me), profile(them), NO_FILTERS).score());
            assertEquals(1.0, scorer.score(profile(them), profile(me), NO_FILTERS).score());
        }

        @Test
        void zeroWithNoGoalsInCommon() {
            me.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);
            them.getStudyGoals().add(StudyGoal.PROBLEM_SOLVING);

            assertEquals(0.0, scorer.score(profile(me), profile(them), NO_FILTERS).score());
        }

        @Test
        void unknownWhenEitherHasNoGoals() {
            me.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);

            assertEquals(CriterionResult.unknown(), scorer.score(profile(me), profile(them), NO_FILTERS));
        }
    }

    @Test
    void explicitGoalScoresCurrentIntentInsteadOfSavedGoalPreferences() {
        me.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);
        them.getStudyGoals().add(StudyGoal.CONCEPT_REVIEW);
        assertEquals(1.0, new StudyGoalScorer().score(profile(me), profile(them),
                new com.studybuddy.matching.MatchSearchCriteria(null, StudyGoal.CONCEPT_REVIEW, null, null, null, null)).score());
    }

    @Test
    void goalSearchStillSeparatesCandidatesByTheSearchersOtherGoals() {
        // Every candidate in a goal search has the searched goal, so it alone cannot rank them.
        var conceptSearch = new com.studybuddy.matching.MatchSearchCriteria(null, StudyGoal.CONCEPT_REVIEW, null, null, null, null);
        me.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);
        them.getStudyGoals().addAll(List.of(StudyGoal.CONCEPT_REVIEW, StudyGoal.EXAM_PREPARATION));
        Student projectFocused = student(3, "Alex");
        projectFocused.getStudyGoals().addAll(List.of(StudyGoal.CONCEPT_REVIEW, StudyGoal.PROJECT_DISCUSSION));
        StudyGoalScorer scorer = new StudyGoalScorer();

        assertEquals(1.0, scorer.score(profile(me), profile(them), conceptSearch).score());
        assertEquals(0.5, scorer.score(profile(me), profile(projectFocused), conceptSearch).score());
    }

    @Nested
    class GroupSize {
        private final GroupSizeScorer scorer = new GroupSizeScorer();

        private double score(int myMin, int myMax, int theirMin, int theirMax) {
            me.setPreferredGroupSizeMin(myMin);
            me.setPreferredGroupSizeMax(myMax);
            them.setPreferredGroupSizeMin(theirMin);
            them.setPreferredGroupSizeMax(theirMax);
            return scorer.score(profile(me), profile(them), NO_FILTERS).score();
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
        void unknownWhenEitherHasNoPreference() {
            me.setPreferredGroupSizeMin(2);
            me.setPreferredGroupSizeMax(3);

            assertEquals(CriterionResult.unknown(), scorer.score(profile(me), profile(them), NO_FILTERS));
        }
    }
}
