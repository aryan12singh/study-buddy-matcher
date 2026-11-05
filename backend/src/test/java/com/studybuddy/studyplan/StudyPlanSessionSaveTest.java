package com.studybuddy.studyplan;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.ConflictException;
import com.studybuddy.course.Course;
import com.studybuddy.course.CourseRepository;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.studysession.SessionSchedule;
import com.studybuddy.studysession.SessionStatus;
import com.studybuddy.studysession.StudySession;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Saving a session roadmap after Gemini answered, when time has passed or the session changed. */
class StudyPlanSessionSaveTest {
    private static final long SESSION_ID = 5L;
    private static final long ACTOR = 1L;
    private static final long VERSION = 2L;
    private static final StudyPlanProperties LIMITS =
            new StudyPlanProperties(DataSize.ofMegabytes(10), 6, 4000, 100000, 40, 5, 5, 16, 40, 3, "Asia/Singapore");
    /** Planned before the first of three Monday meetings (2, 9 and 16 November, 10:00 to 11:30 in Singapore). */
    private static final Instant PLANNED_AT = Instant.parse("2026-10-30T00:00:00Z");
    /** 12:00 in Singapore on 2 November: the first meeting ended while the roadmap was written. */
    private static final Instant SAVED_AT = Instant.parse("2026-11-02T04:00:00Z");
    private static final RoadmapDraft DRAFT = new RoadmapDraft(List.of("Encapsulation"), List.of(
            new RoadmapDraft.Entry(1, "Encapsulation", 60, List.of("Lecture 3")),
            new RoadmapDraft.Entry(2, "Inheritance", 60, List.of("Lecture 4")),
            new RoadmapDraft.Entry(3, "Polymorphism", 60, List.of("Lecture 5"))));

    private final StudyPlanRepository plans = mock(StudyPlanRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final StudyPlanAccess access = mock(StudyPlanAccess.class);
    private final StudySession session = mock(StudySession.class);
    private final Course course = new Course("IS442", "Object Oriented Programming");
    private StudyPlanService service;
    private SessionPlanRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(course, "id", 100L);
        when(session.getId()).thenReturn(SESSION_ID);
        when(session.getVersion()).thenReturn(VERSION);
        when(session.getStatus()).thenReturn(SessionStatus.SCHEDULED);
        when(session.getTimeZone()).thenReturn("Asia/Singapore");
        when(session.schedule()).thenReturn(new SessionSchedule(LocalDateTime.parse("2026-11-02T10:00"),
                LocalDateTime.parse("2026-11-02T11:30"), "Asia/Singapore", 3));
        when(access.lockForManaging(SESSION_ID, ACTOR)).thenReturn(session);
        when(courses.findById(100L)).thenReturn(Optional.of(course));
        when(plans.save(any(StudyPlan.class))).thenAnswer(call -> call.getArgument(0));
        RoadmapFrame planned = new SessionMeetingFrames(LIMITS, Clock.fixed(PLANNED_AT, ZoneOffset.UTC)).upcoming(session);
        request = new SessionPlanRequest(SESSION_ID, VERSION, course, StudyGoal.CONCEPT_REVIEW, planned);
        Clock saveClock = Clock.fixed(SAVED_AT, ZoneOffset.UTC);
        service = new StudyPlanService(plans, courses, mock(AccountAccess.class), access, mock(StudyPlanCalendar.class),
                new SessionMeetingFrames(LIMITS, saveClock), mock(StudyPlanAssembler.class), saveClock);
    }

    @Test
    void aMeetingThatEndedWhileGeminiAnsweredIsLeftOutInsteadOfRefusingTheSave() {
        service.saveSession(ACTOR, request, StudyMaterial.pastedTopics("OOP"), DRAFT);

        ArgumentCaptor<StudyPlan> saved = ArgumentCaptor.forClass(StudyPlan.class);
        verify(plans).save(saved.capture());
        StudyPlan plan = saved.getValue();
        assertEquals(2, plan.getBlockCount());
        assertEquals(LocalDate.of(2026, 11, 9), plan.getFirstBlockDate());
        assertEquals(List.of("Inheritance", "Polymorphism"), plan.getEntries().stream().map(StudyPlanEntry::getTitle).toList());
        assertEquals(List.of(1, 2), plan.getEntries().stream().map(StudyPlanEntry::getSequence).toList());
    }

    @Test
    void aSessionEditedWhileGeminiAnsweredIsRefused() {
        when(session.getVersion()).thenReturn(VERSION + 1);

        assertThrows(ConflictException.class, () -> service.saveSession(ACTOR, request, StudyMaterial.pastedTopics("OOP"), DRAFT));

        verify(plans, never()).save(any());
    }
}
