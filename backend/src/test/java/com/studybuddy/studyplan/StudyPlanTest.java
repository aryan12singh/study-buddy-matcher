package com.studybuddy.studyplan;

import com.studybuddy.ai.AiAttachment;
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
    private static final LocalDate FRIDAY = LocalDate.of(2026, 10, 23);
    private static final LocalDate EXAM = LocalDate.of(2026, 11, 20);
    private static final Course IS442 = new Course("IS442", "Object Oriented Programming");
    private static final StudyMaterial SLIDES = StudyMaterial.uploads(List.of("week3.pdf", "week4.pdf"),
            List.of(new AiAttachment("application/pdf", "%PDF-1.7".getBytes())), "");
    private static final RoadmapFrame TWO_WEEKS = RoadmapFrame.weeks(FRIDAY, 2, 360);
    private static final RoadmapDraft DRAFT = new RoadmapDraft(List.of("Encapsulation", "Polymorphism"), List.of(
            new RoadmapDraft.Entry(1, "Encapsulation", 120, List.of("Re-read lecture 3")),
            new RoadmapDraft.Entry(1, "Inheritance", 60, List.of()),
            new RoadmapDraft.Entry(2, "Polymorphism", 180, List.of("Tutorial 4", "Past paper Q2"))));

    private static Student student() {
        var user = new User("priya@example.test", "test-hash", Role.STUDENT);
        ReflectionTestUtils.setField(user, "id", 11L);
        var student = new Student(user, "Priya", "SCIS", "Information Systems", 3, "Synthetic contact");
        ReflectionTestUtils.setField(student, "id", 11L);
        return student;
    }

    private static StudyPlan personalPlan() {
        return StudyPlan.personal(student(), IS442, StudyGoal.EXAM_PREPARATION, EXAM, TWO_WEEKS, SLIDES, DRAFT, CREATED);
    }

    @Test
    void aPersonalPlanBelongsToItsStudentAndKeepsOnlyTheFileNames() {
        StudyPlan plan = personalPlan();

        assertEquals(StudyPlanScope.PERSONAL, plan.getScope());
        assertEquals(11L, plan.getStudent().getId());
        assertEquals(11L, plan.getCreatedBy());
        assertNull(plan.getSession());
        assertEquals(EXAM, plan.getExamDate());
        assertEquals(FRIDAY, plan.getFirstBlockDate());
        assertEquals(2, plan.getBlockCount());
        assertEquals(360, plan.getMinutesPerBlock());
        assertEquals(StudyMaterialSource.UPLOAD, plan.getSourceKind());
        assertEquals(List.of("week3.pdf", "week4.pdf"), plan.getSourceFilenames());
        assertEquals(List.of("Encapsulation", "Polymorphism"), plan.getSourceTopics());
        assertEquals(CREATED, plan.getCreatedAt());
    }

    @Test
    void aPlanForJustThisWeekNeedsNoExam() {
        StudyPlan plan = StudyPlan.personal(student(), IS442, null, null, RoadmapFrame.weeks(FRIDAY, 1, 240),
                StudyMaterial.pastedTopics("Polymorphism"),
                new RoadmapDraft(List.of("Polymorphism"), List.of(new RoadmapDraft.Entry(1, "Polymorphism", 240, List.of()))),
                CREATED);

        assertNull(plan.getExamDate());
        assertEquals(1, plan.getBlockCount());
        assertTrue(plan.getSourceFilenames().isEmpty());
    }

    @Test
    void entriesTakeTheirBlocksDateAndKeepTheirOrder() {
        List<StudyPlanEntry> entries = personalPlan().getEntries();

        assertEquals(List.of(0, 1, 2), entries.stream().map(StudyPlanEntry::getPosition).toList());
        assertEquals(List.of(1, 1, 2), entries.stream().map(StudyPlanEntry::getSequence).toList());
        assertEquals(List.of(FRIDAY, FRIDAY, FRIDAY.plusDays(7)), entries.stream().map(StudyPlanEntry::getScheduledDate).toList());
        assertEquals(List.of("Tutorial 4", "Past paper Q2"), entries.get(2).getTasks());
        assertFalse(entries.getFirst().isCompleted());
    }

    @Test
    void aSessionPlanBelongsToItsSessionAndHasNoExam() {
        StudySession session = mock(StudySession.class);
        RoadmapFrame meeting = RoadmapFrame.meetings(List.of(LocalDate.of(2026, 10, 29)), 120);
        RoadmapDraft agenda = new RoadmapDraft(List.of("Polymorphism"), List.of(
                new RoadmapDraft.Entry(1, "Warm-up quiz", 15, List.of()), new RoadmapDraft.Entry(1, "Polymorphism", 90, List.of())));

        StudyPlan plan = StudyPlan.forSession(session, IS442, null, meeting, StudyMaterial.pastedTopics("Polymorphism"),
                agenda, 12L, CREATED);

        assertEquals(StudyPlanScope.SESSION, plan.getScope());
        assertSame(session, plan.getSession());
        assertNull(plan.getStudent());
        assertNull(plan.getExamDate());
        assertEquals(12L, plan.getCreatedBy());
        assertEquals(StudyMaterialSource.PASTE, plan.getSourceKind());
        assertEquals(LocalDate.of(2026, 10, 29), plan.getEntries().get(1).getScheduledDate());
    }

    @Test
    void aPlanUsesTheBlockKindOfItsScope() {
        RoadmapFrame meeting = RoadmapFrame.meetings(List.of(FRIDAY), 120);
        RoadmapDraft oneEntry = new RoadmapDraft(List.of("A"), List.of(new RoadmapDraft.Entry(1, "A", 60, List.of())));

        assertThrows(IllegalArgumentException.class, () -> StudyPlan.personal(student(), IS442, null, null, meeting,
                StudyMaterial.pastedTopics("A"), oneEntry, CREATED));
        assertThrows(IllegalArgumentException.class, () -> StudyPlan.forSession(mock(StudySession.class), IS442, null,
                RoadmapFrame.weeks(FRIDAY, 1, 60), StudyMaterial.pastedTopics("A"), oneEntry, 12L, CREATED));
    }

    @Test
    void anExamBeforeThePlanStartsIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> StudyPlan.personal(student(), IS442, null, FRIDAY.minusDays(1),
                TWO_WEEKS, SLIDES, DRAFT, CREATED));
    }

    @Test
    void anEntryOutsideTheFrameIsRefused() {
        RoadmapDraft weekThree = new RoadmapDraft(List.of("A"), List.of(new RoadmapDraft.Entry(3, "A", 60, List.of())));

        assertThrows(IllegalArgumentException.class, () -> StudyPlan.personal(student(), IS442, null, null, TWO_WEEKS,
                SLIDES, weekThree, CREATED));
    }

    @Test
    void tickingAnEntryMarksItAndUpdatesThePlan() {
        StudyPlan plan = personalPlan();
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
        ReflectionTestUtils.setField(plan.getEntries().getFirst(), "id", 5L);

        assertFalse(plan.markEntry(99L, true, TICKED));
        assertEquals(CREATED, plan.getUpdatedAt());
    }

    @Test
    void returnedListsCannotChangeThePlan() {
        StudyPlan plan = personalPlan();

        assertThrows(UnsupportedOperationException.class, () -> plan.getSourceTopics().add("Extra"));
        assertThrows(UnsupportedOperationException.class, () -> plan.getSourceFilenames().add("Extra"));
        assertThrows(UnsupportedOperationException.class, () -> plan.getEntries().clear());
        assertThrows(UnsupportedOperationException.class, () -> plan.getEntries().getFirst().getTasks().add("Extra"));
    }
}
