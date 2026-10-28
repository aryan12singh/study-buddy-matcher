package com.studybuddy.assistant;

import com.studybuddy.course.CourseDto;
import com.studybuddy.studygroup.StudyGroupService;
import com.studybuddy.studygroup.StudyGroupSummaryDto;
import com.studybuddy.studyplan.SessionPlanNotFoundException;
import com.studybuddy.studyplan.StudyMaterialSource;
import com.studybuddy.studyplan.StudyPlanDto;
import com.studybuddy.studyplan.StudyPlanEntryDto;
import com.studybuddy.studyplan.StudyPlanScope;
import com.studybuddy.studyplan.StudyPlanService;
import com.studybuddy.studysession.SessionDto;
import com.studybuddy.studysession.SessionOccurrenceDto;
import com.studybuddy.studysession.SessionScope;
import com.studybuddy.studysession.SessionStatus;
import com.studybuddy.studysession.StudySessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionIntentsTest {
    private static final long ME = 7L;
    /** Friday 30 October 2026, before both sessions' first meetings. */
    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-30T00:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);
    private static final LocalDate THURSDAY = LocalDate.of(2026, 11, 5);

    private final StudySessionService sessionService = mock(StudySessionService.class);
    private final StudyGroupService groupService = mock(StudyGroupService.class);
    private final StudyPlanService plans = mock(StudyPlanService.class);
    private final AssistantSessions sessions = new AssistantSessions(sessionService, groupService, NOW);
    private final PlanSessionIntent planSession = new PlanSessionIntent(sessions);
    private final SessionTopicsIntent topics = new SessionTopicsIntent(sessions, plans);

    /** 10:00 to 11:00 in Singapore, weekly for two weeks from the given day. */
    private static SessionDto session(long id, Long groupId, String title, LocalDate first, boolean canManage) {
        Instant start = first.atTime(2, 0).toInstant(ZoneOffset.UTC);
        List<SessionOccurrenceDto> meetings = List.of(new SessionOccurrenceDto(start, start.plusSeconds(3600)),
                new SessionOccurrenceDto(start.plusSeconds(7 * 86400), start.plusSeconds(7 * 86400 + 3600)));
        return new SessionDto(id, 0, groupId == null ? SessionScope.BUDDY : SessionScope.GROUP, groupId, groupId == null ? 3L : null,
                title, LocalDateTime.of(first, LocalTime.of(10, 0)), LocalDateTime.of(first, LocalTime.of(11, 0)),
                start, start.plusSeconds(3600), "Asia/Singapore", 2, null, null, null, SessionStatus.SCHEDULED, canManage,
                meetings, meetings.getFirst(), Instant.parse("2026-10-20T00:00:00Z"), null);
    }

    private static StudyGroupSummaryDto group(long id, String name, String courseCode) {
        return new StudyGroupSummaryDto(id, name, 4L, courseCode, "Course", 1L, "Leader", null, Set.of(), 4, 3, true, null);
    }

    private static StudyPlanDto roadmap(long sessionId, StudyPlanEntryDto... entries) {
        return new StudyPlanDto(30L, StudyPlanScope.SESSION, new CourseDto(4L, "IS442", "Object Oriented Programming"), sessionId,
                MONDAY, 2, 60, null, null, StudyMaterialSource.PASTE, List.of(), List.of("Polymorphism"), true, List.of(entries),
                Instant.parse("2026-10-25T00:00:00Z"), Instant.parse("2026-10-25T00:00:00Z"));
    }

    private static StudyPlanEntryDto entry(LocalDate day, String title, int minutes, boolean completed) {
        return new StudyPlanEntryDto(1L, 1, day, title, minutes, List.of(), completed);
    }

    private static AssistantRequest asking(AssistantIntentType intent, String groupName, String courseCode, LocalDate day) {
        return new AssistantRequest(intent, courseCode, null, null, null, null, null, groupName, null, null, null, null, null, day);
    }

    @BeforeEach
    void twoSessions() {
        // The design patterns group meets on Mondays and the stats group on Thursdays; soonest first, as the service lists them.
        when(sessionService.mine(ME)).thenReturn(List.of(
                session(11L, 5L, "Revision", MONDAY, true),
                session(12L, 6L, "Finals prep", THURSDAY, false)));
        when(groupService.mine(ME)).thenReturn(List.of(group(5L, "IS442 Design Patterns Clinic", "IS442"),
                group(6L, "Stats Finals Crunch", "COR-STAT1202")));
    }

    // Plan a session

    @Test
    void sendsTheLeaderToTheSoonestSessionToPlanIt() {
        AssistantReplyDto reply = planSession.reply(ME, asking(AssistantIntentType.PLAN_SESSION, null, null, null));

        assertTrue(reply.text().startsWith("Let's plan Revision (IS442 Design Patterns Clinic), next meeting on Monday 2 November."), reply.text());
        assertTrue(reply.text().contains("That's your soonest session"), reply.text());
        assertEquals("/sessions/11", reply.action().link());
    }

    @Test
    void findsTheSessionOfTheGroupNamedAndSaysWhoMayPlanIt() {
        AssistantReplyDto reply = planSession.reply(ME, asking(AssistantIntentType.PLAN_SESSION, "stats", null, null));

        assertEquals("Only the group leader can plan Finals prep (Stats Finals Crunch). Once they have, its roadmap shows on the session page.",
                reply.text());
        assertEquals("/sessions/12", reply.action().link());
    }

    @Test
    void saysWhenThereIsNoSessionToPlan() {
        when(sessionService.mine(ME)).thenReturn(List.of());

        AssistantReplyDto reply = planSession.reply(ME, asking(AssistantIntentType.PLAN_SESSION, null, null, null));

        assertTrue(reply.text().startsWith("You don't have any upcoming study sessions to plan yet."), reply.text());
        assertEquals("/sessions", reply.action().link());
    }

    // What a session covers

    @Test
    void listsTheTopicsOfTheMeetingOnTheDayAsked() {
        when(plans.getForSession(12L, ME)).thenReturn(roadmap(12L,
                entry(THURSDAY, "Hypothesis testing", 45, true), entry(THURSDAY, "Confidence intervals", 30, false),
                entry(THURSDAY.plusWeeks(1), "Regression", 60, false)));

        AssistantReplyDto reply = topics.reply(ME, asking(AssistantIntentType.SESSION_TOPICS, null, null, THURSDAY));

        assertEquals("On Thursday 5 November, Finals prep (Stats Finals Crunch) covers 2 topics:", reply.text());
        assertEquals(List.of("✓ Hypothesis testing, 45 minutes", "Confidence intervals, 30 minutes"),
                reply.todos().stream().map(AssistantTodoDto::text).toList());
        assertEquals("/sessions/12", reply.todos().getFirst().link());
    }

    @Test
    void withoutADayItIsTheSoonestMeeting() {
        when(plans.getForSession(11L, ME)).thenReturn(roadmap(11L, entry(MONDAY, "Polymorphism", 60, false)));

        AssistantReplyDto reply = topics.reply(ME, asking(AssistantIntentType.SESSION_TOPICS, null, "IS442", null));

        assertEquals("On Monday 2 November, Revision (IS442 Design Patterns Clinic) covers 1 topic:", reply.text());
    }

    @Test
    void saysASessionHasNoRoadmapYetAndWhoCanMakeOne() {
        when(plans.getForSession(anyLong(), anyLong())).thenThrow(new SessionPlanNotFoundException(12L));

        AssistantReplyDto reply = topics.reply(ME, asking(AssistantIntentType.SESSION_TOPICS, "stats", null, null));

        assertEquals("Finals prep (Stats Finals Crunch) meets on Thursday 5 November but has no roadmap yet. The group leader can make one.",
                reply.text());
        assertTrue(reply.todos().isEmpty());
    }

    @Test
    void saysSoWhenNoSessionMeetsThatDay() {
        AssistantReplyDto reply = topics.reply(ME, asking(AssistantIntentType.SESSION_TOPICS, null, null, LocalDate.of(2026, 11, 4)));

        assertEquals("You have no upcoming study session on Wednesday 4 November that I can find.", reply.text());
        verify(plans, never()).getForSession(anyLong(), anyLong());
    }

    @Test
    void saysSoWhenTheRoadmapNoLongerMatchesTheMeeting() {
        when(plans.getForSession(11L, ME)).thenReturn(roadmap(11L, entry(MONDAY.minusDays(1), "Polymorphism", 60, false)));

        AssistantReplyDto reply = topics.reply(ME, asking(AssistantIntentType.SESSION_TOPICS, "design patterns", null, null));

        assertTrue(reply.text().contains("doesn't cover its meeting on Monday 2 November"), reply.text());
        assertTrue(reply.todos().isEmpty());
    }
}
