package com.studybuddy.studyplan;

import com.studybuddy.course.Course;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.studysession.StudySession;
import com.studybuddy.user.Role;
import com.studybuddy.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class StudyPlanTest {
    private static final Instant CREATED = Instant.parse("2026-10-23T02:00:00Z");
    private static final Instant TICKED = Instant.parse("2026-10-24T02:00:00Z");
    private static final LocalDate EXAM = LocalDate.of(2026, 11, 20);
    private static final Course IS442 = new Course("IS442", "Object Oriented Programming");
    private static final StudyMaterial PDF = StudyMaterial.pdf("week3.pdf", "%PDF-1.7".getBytes());

    private static Student student() {
        var user = new User("priya@example.test", "test-hash", Role.STUDENT);
        ReflectionTestUtils.setField(user, "id", 11L);
        var student = new Student(user, "Priya", "SCIS", "Information Systems", 3, "Synthetic contact");
        ReflectionTestUtils.setField(student, "id", 11L);
        return student;
    }

    private static StudyPlan personalPlan() {
        return StudyPlan.personal(student(), IS442, EXAM, 6, StudyGoal.EXAM_PREPARATION, PDF,
                List.of("Encapsulation", "Polymorphism"), CREATED);
    }

    @Test
    void aPersonalPlanBelongsToItsStudentAndKeepsOnlyTheFilename() {
        StudyPlan plan = personalPlan();

        assertEquals(StudyPlanScope.PERSONAL, plan.getScope());
        assertEquals(11L, plan.getStudent().getId());
        assertEquals(11L, plan.getCreatedBy());
        assertNull(plan.getSession());
        assertEquals(EXAM, plan.getExamDate());
        assertEquals(6, plan.getHoursPerWeek());
        assertEquals(StudyMaterialSource.UPLOAD, plan.getSourceKind());
        assertEquals("week3.pdf", plan.getSourceFilename());
        assertEquals(List.of("Encapsulation", "Polymorphism"), plan.getSourceTopics());
        assertEquals(CREATED, plan.getCreatedAt());
    }

    @Test
    void aSessionPlanBelongsToItsSessionAndHasNoExam() {
        StudySession session = mock(StudySession.class);

        StudyPlan plan = StudyPlan.forSession(session, IS442, null, StudyMaterial.pastedTopics("Polymorphism"),
                List.of("Polymorphism"), 12L, CREATED);

        assertEquals(StudyPlanScope.SESSION, plan.getScope());
        assertSame(session, plan.getSession());
        assertNull(plan.getStudent());
        assertNull(plan.getExamDate());
        assertNull(plan.getHoursPerWeek());
        assertEquals(12L, plan.getCreatedBy());
        assertEquals(StudyMaterialSource.PASTE, plan.getSourceKind());
        assertNull(plan.getSourceFilename());
    }

    @Test
    void aPersonalPlanNeedsAnExamDateAndWeeklyHours() {
        assertThrows(IllegalArgumentException.class, () -> StudyPlan.personal(student(), IS442, null, 6, null, PDF,
                List.of("Polymorphism"), CREATED));
        assertThrows(IllegalArgumentException.class, () -> StudyPlan.personal(student(), IS442, EXAM, 0, null, PDF,
                List.of("Polymorphism"), CREATED));
    }

    @Test
    void entriesKeepTheOrderTheyWereAddedIn() {
        StudyPlan plan = personalPlan();

        plan.addEntry(1, null, "Encapsulation", 120, List.of("Re-read lecture 2"));
        plan.addEntry(1, null, "Inheritance", 60, List.of());
        plan.addEntry(2, null, "Polymorphism", 180, List.of("Tutorial 3", "Past paper Q2"));

        List<StudyPlanEntry> entries = plan.getEntries();
        assertEquals(List.of(0, 1, 2), entries.stream().map(StudyPlanEntry::getPosition).toList());
        assertEquals(List.of(1, 1, 2), entries.stream().map(StudyPlanEntry::getSequence).toList());
        assertEquals(List.of("Tutorial 3", "Past paper Q2"), entries.get(2).getTasks());
        assertSame(plan, entries.getFirst().getPlan());
        assertFalse(entries.getFirst().isCompleted());
    }

    @Test
    void anEntryNeedsAPositiveWeekAndPlannedTime() {
        StudyPlan plan = personalPlan();

        assertThrows(IllegalArgumentException.class, () -> plan.addEntry(0, null, "Encapsulation", 60, List.of()));
        assertThrows(IllegalArgumentException.class, () -> plan.addEntry(1, null, "Encapsulation", 0, List.of()));
    }

    @Test
    void tickingAnEntryMarksItAndUpdatesThePlan() {
        StudyPlan plan = personalPlan();
        plan.addEntry(1, null, "Encapsulation", 120, List.of());
        ReflectionTestUtils.setField(plan.getEntries().getFirst(), "id", 5L);

        assertTrue(plan.markEntry(5L, true, TICKED));

        assertTrue(plan.getEntries().getFirst().isCompleted());
        assertEquals(TICKED, plan.getUpdatedAt());
        assertTrue(plan.markEntry(5L, false, TICKED));
        assertFalse(plan.getEntries().getFirst().isCompleted());
    }

    @Test
    void anEntryFromAnotherPlanIsNotTicked() {
        StudyPlan plan = personalPlan();
        plan.addEntry(1, null, "Encapsulation", 120, List.of());
        ReflectionTestUtils.setField(plan.getEntries().getFirst(), "id", 5L);

        assertFalse(plan.markEntry(99L, true, TICKED));
        assertEquals(CREATED, plan.getUpdatedAt());
    }

    @Test
    void returnedListsCannotChangeThePlan() {
        StudyPlan plan = personalPlan();
        plan.addEntry(1, null, "Encapsulation", 120, List.of("Re-read lecture 2"));

        assertThrows(UnsupportedOperationException.class, () -> plan.getSourceTopics().add("Extra"));
        assertThrows(UnsupportedOperationException.class, () -> plan.getEntries().clear());
        assertThrows(UnsupportedOperationException.class, () -> plan.getEntries().getFirst().getTasks().add("Extra"));
    }
}
