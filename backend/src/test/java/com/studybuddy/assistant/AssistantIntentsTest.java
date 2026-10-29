package com.studybuddy.assistant;

import com.studybuddy.course.Course;
import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.matching.MatchSearchResultDto;
import com.studybuddy.matching.MatchService;
import com.studybuddy.matching.MatchSort;
import com.studybuddy.matching.SharedSlotDto;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyMode;
import com.studybuddy.studygroup.StudyGroupService;
import com.studybuddy.studygroup.StudyGroupSummaryDto;
import com.studybuddy.studysession.SessionAvailabilityDto;
import com.studybuddy.studysession.StudySessionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssistantIntentsTest {
    private static final AssistantProperties PROPERTIES = new AssistantProperties(500, 3, "Asia/Singapore", "lite", 400, java.time.Duration.ofSeconds(20), 200, 30);
    private static final Course IS442 = course(4L, "IS442", "Object Oriented Programming");
    private final AssistantCourses courses = mock(AssistantCourses.class);

    private static Course course(long id, String code, String name) {
        Course course = new Course(code, name);
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }

    private static AssistantRequest request(AssistantIntentType intent, String courseCode, StudyMode mode,
            GroupSizePreference size, Integer minHours, TimeOfDay time, String groupName, Integer minutes,
            LocalDate exam, Integer hoursPerWeek, String topic) {
        return new AssistantRequest(intent, courseCode, null, mode, size, minHours, time, groupName, minutes, exam, hoursPerWeek, topic, null, null);
    }

    private void knows(Course course) {
        when(courses.byCode(any())).thenReturn(Optional.empty());
        when(courses.byCode(course.getCode())).thenReturn(Optional.of(course));
    }

    // Find buddies

    @Test
    void findBuddiesSearchesWithTheStatedFiltersAndSaysHowManyItFound() {
        knows(IS442);
        MatchService matches = mock(MatchService.class);
        when(matches.search(any(), anyLong())).thenReturn(new MatchSearchResultDto(MatchingStrategyType.BALANCED, 1, MatchSort.MOST_SHARED_HOURS, 3, 5, List.of()));
        var intent = new FindBuddiesIntent(matches, courses, PROPERTIES);

        AssistantReplyDto reply = intent.reply(7L, request(AssistantIntentType.FIND_BUDDIES, "IS442", StudyMode.IN_PERSON,
                GroupSizePreference.SMALL_GROUP, null, TimeOfDay.EVENING, null, null, null, null, null));

        ArgumentCaptor<MatchSearchCriteria> criteria = ArgumentCaptor.forClass(MatchSearchCriteria.class);
        verify(matches).search(criteria.capture(), any());
        assertEquals(4L, criteria.getValue().courseId());
        assertEquals(StudyMode.IN_PERSON, criteria.getValue().studyMode());
        assertEquals(GroupSizePreference.SMALL_GROUP, criteria.getValue().groupSize());
        assertEquals(MatchSort.MOST_SHARED_HOURS, criteria.getValue().sort(), "Time of day falls back to shared free time");
        assertEquals(3, criteria.getValue().limit());
        assertTrue(reply.text().startsWith("I found 5 matches for IS442."));
        assertTrue(reply.text().contains("can't filter by time of day"));
        assertEquals(List.of("IS442 Object Oriented Programming", "In person", "Small group"), reply.filters());
    }

    @Test
    void findBuddiesAsksForACourseInsteadOfGuessingOne() {
        knows(IS442);
        MatchService matches = mock(MatchService.class);
        var intent = new FindBuddiesIntent(matches, courses, PROPERTIES);

        AssistantReplyDto noCourse = intent.reply(7L, request(AssistantIntentType.FIND_BUDDIES, null, null, null, null, null, null, null, null, null, null));
        AssistantReplyDto unknown = intent.reply(7L, request(AssistantIntentType.FIND_BUDDIES, "XX999", null, null, null, null, null, null, null, null, null));

        assertTrue(noCourse.text().startsWith("Which course"));
        assertTrue(unknown.text().contains("couldn't find a course called “XX999”"));
        verify(matches, never()).search(any(), anyLong());
    }

    // When to meet

    private static StudyGroupSummaryDto group(long id, String name, String code, boolean active) {
        return new StudyGroupSummaryDto(id, name, 4L, code, "Course", 1L, "Lead", StudyMode.IN_PERSON, Set.of(), 5, 4, active, null);
    }

    private static final SharedSlotDto ONE_HOUR = new SharedSlotDto(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
    private static final SharedSlotDto THREE_HOURS = new SharedSlotDto(DayOfWeek.THURSDAY, LocalTime.of(14, 0), LocalTime.of(17, 0));

    @Test
    void whenToMeetListsSharedSlotsLongEnoughForTheMeeting() {
        StudyGroupService groups = mock(StudyGroupService.class);
        StudySessionService sessions = mock(StudySessionService.class);
        when(groups.mine(7L)).thenReturn(List.of(group(1L, "IS442 Design Patterns Clinic", "IS442", true), group(2L, "Stats Crunch", "ST101", true)));
        when(sessions.groupAvailability(1L, 7L)).thenReturn(new SessionAvailabilityDto("Asia/Singapore", 4, List.of(ONE_HOUR, THREE_HOURS), 240));
        var intent = new WhenToMeetIntent(groups, sessions);

        AssistantReplyDto reply = intent.reply(7L, request(AssistantIntentType.WHEN_TO_MEET, null, null, null, null, null, "design patterns", 120, null, null, null));

        assertEquals(List.of(THREE_HOURS), reply.slots());
        assertTrue(reply.text().startsWith("All 4 members of IS442 Design Patterns Clinic are free at this time each week for 2 hours"));
        assertEquals("/groups/1/sessions/new", reply.action().link());
    }

    @Test
    void whenToMeetAsksWhichGroupWhenItCannotTell() {
        StudyGroupService groups = mock(StudyGroupService.class);
        StudySessionService sessions = mock(StudySessionService.class);
        when(groups.mine(7L)).thenReturn(List.of(group(1L, "Design Patterns", "IS442", true), group(2L, "Stats Crunch", "ST101", true),
                group(3L, "Old Closed Group", "IS111", false)));
        var intent = new WhenToMeetIntent(groups, sessions);

        AssistantReplyDto reply = intent.reply(7L, request(AssistantIntentType.WHEN_TO_MEET, null, null, null, null, null, null, null, null, null, null));

        assertTrue(reply.text().startsWith("Which group do you mean? You're in Design Patterns, Stats Crunch."), "Closed groups are left out");
        verify(sessions, never()).groupAvailability(anyLong(), anyLong());
    }

    @Test
    void whenToMeetSaysPlainlyWhenNoTimeIsLongEnough() {
        StudyGroupService groups = mock(StudyGroupService.class);
        StudySessionService sessions = mock(StudySessionService.class);
        when(groups.mine(7L)).thenReturn(List.of(group(1L, "Design Patterns", "IS442", true)));
        when(sessions.groupAvailability(1L, 7L)).thenReturn(new SessionAvailabilityDto("Asia/Singapore", 4, List.of(ONE_HOUR), 60));
        var intent = new WhenToMeetIntent(groups, sessions);

        AssistantReplyDto reply = intent.reply(7L, request(AssistantIntentType.WHEN_TO_MEET, null, null, null, null, null, null, 90, null, null, null));

        assertEquals("Design Patterns has no weekly time when everyone is free for 1 hour 30 minutes. Try a shorter session.", reply.text());
        assertTrue(reply.slots().isEmpty());
    }

    @Test
    void whenToMeetPointsAStudentWithNoGroupsToTheGroupList() {
        StudyGroupService groups = mock(StudyGroupService.class);
        when(groups.mine(7L)).thenReturn(List.of());

        AssistantReplyDto reply = new WhenToMeetIntent(groups, mock(StudySessionService.class)).reply(7L,
                request(AssistantIntentType.WHEN_TO_MEET, null, null, null, null, null, null, null, null, null, null));

        assertEquals("/groups", reply.action().link());
    }

    // Plan study, quiz me, unknown

    @Test
    void planStudyLinksToStudyPlansWithTheStatedDetails() {
        knows(IS442);

        AssistantReplyDto reply = new PlanStudyIntent(courses).reply(7L, request(AssistantIntentType.PLAN_STUDY, "IS442", null,
                null, null, null, null, null, LocalDate.of(2026, 11, 20), 6, null));

        assertEquals("/study-plan?courseId=4&examDate=2026-11-20&hoursPerWeek=6", reply.action().link());
        assertEquals("Let's plan your IS442 revision, with the exam on Friday 20 November, 6 hours a week. "
                + "Open Study plans, attach your notes and I'll draft a week-by-week plan.", reply.text());
    }

    @Test
    void quizMeHandsOverToQuizBuddyWithTheTopic() {
        knows(IS442);

        AssistantReplyDto reply = new QuizMeIntent(courses).reply(7L, request(AssistantIntentType.QUIZ_ME, "IS442", null,
                null, null, null, null, null, null, null, "polymorphism"));

        assertEquals(AssistantActionKind.OPEN_QUIZ_BUDDY, reply.action().kind());
        assertEquals("polymorphism", reply.action().prefill().get("topic"));
        assertEquals("4", reply.action().prefill().get("courseId"));
        assertNull(reply.action().link());
    }

    @Test
    void unknownSaysWhatTheAssistantCanDo() {
        AssistantReplyDto reply = new UnknownIntent().reply(7L, AssistantRequest.unknown());

        assertEquals(UnknownIntent.HELP, reply.text());
    }
}
