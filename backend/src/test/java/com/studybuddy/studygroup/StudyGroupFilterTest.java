package com.studybuddy.studygroup;

import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.studybuddy.studygroup.StudyGroupFixtures.course;
import static com.studybuddy.studygroup.StudyGroupFixtures.group;
import static com.studybuddy.studygroup.StudyGroupFixtures.student;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudyGroupFilterTest {

    private StudyGroup group;

    @BeforeEach
    void setUp() {
        group = group(5L, course(1L, "IS442"), student(1L, "Alice"), 4);
        group.replaceStudyGoals(Set.of(StudyGoal.EXAM_PREPARATION, StudyGoal.PROBLEM_SOLVING));
        group.setPreferredStudyMode(StudyMode.IN_PERSON);
    }

    @Test
    void emptyFilterMatchesEveryGroup() {
        assertTrue(StudyGroupFilter.none().matches(group));
    }

    @Test
    void courseFilterMatchesOnlyThatCourse() {
        assertTrue(new StudyGroupFilter(1L, null, null).matches(group));
        assertFalse(new StudyGroupFilter(2L, null, null).matches(group));
    }

    @Test
    void studyGoalFilterMatchesAnyOfTheGroupsGoals() {
        assertTrue(new StudyGroupFilter(null, StudyGoal.PROBLEM_SOLVING, null).matches(group));
        assertFalse(new StudyGroupFilter(null, StudyGoal.PROJECT_DISCUSSION, null).matches(group));
    }

    @Test
    void studyModeFilterMatchesExactMode() {
        assertTrue(new StudyGroupFilter(null, null, StudyMode.IN_PERSON).matches(group));
        assertFalse(new StudyGroupFilter(null, null, StudyMode.ONLINE).matches(group));
    }

    @Test
    void eitherModeIsCompatibleBothWays() {
        assertTrue(new StudyGroupFilter(null, null, StudyMode.EITHER).matches(group));

        group.setPreferredStudyMode(StudyMode.EITHER);
        assertTrue(new StudyGroupFilter(null, null, StudyMode.ONLINE).matches(group));
    }

    @Test
    void allFiltersMustMatch() {
        assertFalse(new StudyGroupFilter(1L, StudyGoal.EXAM_PREPARATION, StudyMode.ONLINE).matches(group));
        assertTrue(new StudyGroupFilter(1L, StudyGoal.EXAM_PREPARATION, StudyMode.IN_PERSON).matches(group));
    }
}
