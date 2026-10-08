package com.studybuddy.studygroup;

import com.studybuddy.student.StudyGoal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.studybuddy.studygroup.StudyGroupFixtures.course;
import static com.studybuddy.studygroup.StudyGroupFixtures.group;
import static com.studybuddy.studygroup.StudyGroupFixtures.student;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudyGroupTest {

    private StudyGroup group;

    @BeforeEach
    void setUp() {
        group = group(5L, course(1L, "IS442"), student(1L, "Alice"), 3);
    }

    @Test
    void newGroupIsActive() {
        assertTrue(group.isActive());
    }

    @Test
    void closeMakesGroupInactive() {
        group.close();

        assertFalse(group.isActive());
    }

    @Test
    void closeThrowsWhenAlreadyClosed() {
        group.close();

        assertThrows(IllegalStateException.class, group::close);
    }

    @Test
    void isLeaderIsTrueOnlyForLeader() {
        assertTrue(group.isLeader(1L));
        assertFalse(group.isLeader(2L));
    }

    @Test
    void hasRoomUntilMemberCountReachesMaxSize() {
        assertTrue(group.hasRoomFor(2));
        assertFalse(group.hasRoomFor(3));
    }

    @Test
    void replaceStudyGoalsDropsOldGoals() {
        group.replaceStudyGoals(Set.of(StudyGoal.CONCEPT_REVIEW));
        group.replaceStudyGoals(Set.of(StudyGoal.EXAM_PREPARATION));

        assertEquals(Set.of(StudyGoal.EXAM_PREPARATION), group.getStudyGoals());
    }
}
