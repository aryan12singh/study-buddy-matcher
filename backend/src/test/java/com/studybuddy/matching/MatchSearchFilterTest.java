package com.studybuddy.matching;

import com.studybuddy.matching.scoring.MatchingProfile;
import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.ProfileProperties;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import org.junit.jupiter.api.Test;

import static com.studybuddy.matching.MatchingFixtures.course;
import static com.studybuddy.matching.MatchingFixtures.profile;
import static com.studybuddy.matching.MatchingFixtures.slot;
import static com.studybuddy.matching.MatchingFixtures.student;
import static java.time.DayOfWeek.MONDAY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchSearchFilterTest {

    private final MatchSearchFilter filter = new MatchSearchFilter(new ProfileProperties(5, 8));
    private final MatchingProfile searcher = profile(student(1, "Priya"), slot(MONDAY, "09:00", "12:00"));
    private final Student them = student(2, "Jamie");

    private boolean accepts(MatchSearchCriteria search) {
        return filter.accepts(search, searcher, profile(them, slot(MONDAY, "10:00", "13:00")));
    }

    @Test
    void courseSearchKeepsStudentsWhoTakeOrTargetTheCourse() {
        MatchSearchCriteria search = new MatchSearchCriteria(4L, null, null, null, null, null);
        assertFalse(accepts(search));

        them.getCoursesTaken().add(course(4, "IS442"));
        assertTrue(accepts(search));

        them.getCoursesTaken().clear();
        them.setTargetCourse(course(4, "IS442"));
        assertTrue(accepts(search));
    }

    @Test
    void goalSearchKeepsStudentsWithThatGoal() {
        MatchSearchCriteria search = new MatchSearchCriteria(null, StudyGoal.EXAM_PREPARATION, null, null, null, null);
        assertFalse(accepts(search));

        them.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);
        assertTrue(accepts(search));
    }

    @Test
    void studyModeFilterAcceptsTheSameModeOrEither() {
        them.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);
        MatchSearchCriteria online = new MatchSearchCriteria(null, StudyGoal.EXAM_PREPARATION, StudyMode.ONLINE, null, null, null);

        assertFalse(accepts(online), "no stated mode cannot be confirmed as online");
        them.setPreferredStudyMode(StudyMode.IN_PERSON);
        assertFalse(accepts(online));
        them.setPreferredStudyMode(StudyMode.EITHER);
        assertTrue(accepts(online));
        them.setPreferredStudyMode(StudyMode.ONLINE);
        assertTrue(accepts(online));
    }

    @Test
    void groupSizeFilterKeepsOverlappingRanges() {
        them.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);
        MatchSearchCriteria oneToOne = new MatchSearchCriteria(null, StudyGoal.EXAM_PREPARATION, null, GroupSizePreference.ONE_TO_ONE, null, null);
        MatchSearchCriteria smallGroup = new MatchSearchCriteria(null, StudyGoal.EXAM_PREPARATION, null, GroupSizePreference.SMALL_GROUP, null, null);

        assertFalse(accepts(oneToOne), "no stated range cannot be confirmed");

        them.setPreferredGroupSizeMin(2);
        them.setPreferredGroupSizeMax(2);
        assertTrue(accepts(oneToOne));
        assertFalse(accepts(smallGroup));

        them.setPreferredGroupSizeMax(4);
        assertTrue(accepts(oneToOne), "2 to 4 people includes a pair");
        assertTrue(accepts(smallGroup));

        them.setPreferredGroupSizeMin(3);
        assertFalse(accepts(oneToOne));
    }

    @Test
    void eitherGroupSizeFiltersNothing() {
        them.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);

        assertTrue(accepts(new MatchSearchCriteria(null, StudyGoal.EXAM_PREPARATION, null, GroupSizePreference.EITHER, null, null)));
    }

    @Test
    void sharedHoursFilterNeedsEnoughOverlap() {
        them.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);

        // The searcher and candidate share 10:00-12:00, two hours.
        assertTrue(accepts(new MatchSearchCriteria(null, StudyGoal.EXAM_PREPARATION, null, null, 2, null)));
        assertFalse(accepts(new MatchSearchCriteria(null, StudyGoal.EXAM_PREPARATION, null, null, 3, null)));
    }
}
