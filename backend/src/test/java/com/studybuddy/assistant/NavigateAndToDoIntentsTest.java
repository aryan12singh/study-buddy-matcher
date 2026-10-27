package com.studybuddy.assistant;

import com.studybuddy.matchrequest.MatchRequestDto;
import com.studybuddy.matchrequest.MatchRequestService;
import com.studybuddy.matchrequest.MatchRequestStatus;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.quiz.QuizAttemptState;
import com.studybuddy.quiz.QuizDto;
import com.studybuddy.quiz.QuizScope;
import com.studybuddy.quiz.QuizService;
import com.studybuddy.quiz.QuizStatus;
import com.studybuddy.student.StudyMode;
import com.studybuddy.studygroup.GroupJoinRequestDto;
import com.studybuddy.studygroup.GroupJoinRequestService;
import com.studybuddy.studygroup.StudyGroupService;
import com.studybuddy.studygroup.StudyGroupSummaryDto;
import com.studybuddy.studysession.SessionDto;
import com.studybuddy.studysession.SessionOccurrenceDto;
import com.studybuddy.studysession.StudySessionService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NavigateAndToDoIntentsTest {
    private static final long ME = 7L;
    private static final Instant NOW = Instant.parse("2026-10-27T04:00:00Z");

    private static AssistantRequest navigateTo(AppPage page) {
        return new AssistantRequest(AssistantIntentType.NAVIGATE, null, null, null, null, null, null, null, null, null, null, null, page);
    }

    @Test
    void navigateNamesThePageWhatItIsForAndLinksToItForThisStudent() {
        AssistantReplyDto reply = new NavigateIntent().reply(ME, navigateTo(AppPage.EDIT_PROFILE));

        assertEquals("That's on Edit my profile: change your courses, study goals, study mode, preferred group size "
                + "and weekly availability.", reply.text());
        assertEquals("/students/7/edit", reply.action().link());
        assertEquals("Go to Edit my profile", reply.action().label());
    }

    @Test
    void navigateOffersHelpWhenNoPageFits() {
        AssistantReplyDto reply = new NavigateIntent().reply(ME, navigateTo(null));

        assertEquals("/help", reply.action().link());
    }

    // To-do

    private final MatchRequestService requests = mock(MatchRequestService.class);
    private final StudyGroupService groups = mock(StudyGroupService.class);
    private final GroupJoinRequestService applications = mock(GroupJoinRequestService.class);
    private final QuizService quizzes = mock(QuizService.class);
    private final StudySessionService sessions = mock(StudySessionService.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final MyToDoIntent todo = new MyToDoIntent(requests, groups, applications, quizzes, sessions, notifications,
            new AssistantProperties(500, 3, "Asia/Singapore", "lite", 400, java.time.Duration.ofSeconds(20), 200, 30), Clock.fixed(NOW, ZoneOffset.UTC));

    private static StudyGroupSummaryDto group(long id, String name, long leaderId) {
        return new StudyGroupSummaryDto(id, name, 4L, "IS442", "OOP", leaderId, "Lead", StudyMode.IN_PERSON, Set.of(), 5, 3, true, null);
    }

    private static MatchRequestDto request(String from, MatchRequestStatus status) {
        return new MatchRequestDto(1L, 2L, from, ME, "Me", "Hi", status, NOW, null, null);
    }

    private static QuizDto quiz(QuizStatus status, QuizAttemptState mine) {
        return new QuizDto(1L, QuizScope.GROUP, null, 1L, null, "week4.pdf", status, 1L, NOW, NOW.plusSeconds(900), null, 10, 0, mine, false);
    }

    private static SessionDto session(long id, String title, Instant start) {
        return new SessionDto(id, 0, null, 1L, null, title, null, null, start, start.plusSeconds(3600), "Asia/Singapore", 1,
                null, null, null, null, false, List.of(), new SessionOccurrenceDto(start, start.plusSeconds(3600)), NOW, null);
    }

    @Test
    void listsEveryKindOfPendingItemWithAButtonToDealWithIt() {
        when(requests.listIncoming(ME)).thenReturn(List.of(request("Marcus Lim", MatchRequestStatus.PENDING),
                request("Old Request", MatchRequestStatus.ACCEPTED)));
        when(groups.mine(ME)).thenReturn(List.of(group(1L, "Design Patterns", ME), group(2L, "Stats Crunch", 99L)));
        when(applications.listPending(1L, ME)).thenReturn(List.of(mock(GroupJoinRequestDto.class), mock(GroupJoinRequestDto.class)));
        when(quizzes.list(1L, ME)).thenReturn(List.of(quiz(QuizStatus.OPEN, QuizAttemptState.SUBMITTED)));
        when(quizzes.list(2L, ME)).thenReturn(List.of(quiz(QuizStatus.OPEN, QuizAttemptState.NOT_STARTED), quiz(QuizStatus.CLOSED, QuizAttemptState.NOT_STARTED)));
        when(sessions.mine(ME)).thenReturn(List.of(session(5L, "IS442 revision", NOW.plus(Duration.ofDays(1))),
                session(6L, "Far away", NOW.plus(Duration.ofDays(20)))));
        when(notifications.unreadCount(ME)).thenReturn(6L);

        AssistantReplyDto reply = todo.reply(ME, AssistantRequest.unknown());

        assertEquals("Here's what's waiting for you:", reply.text());
        assertEquals(List.of(
                new AssistantTodoDto("1 buddy request from Marcus Lim waiting for your reply", "Review requests", "/connections?view=incoming"),
                new AssistantTodoDto("2 applications to join Design Patterns", "Review applications", "/groups/1/manage"),
                new AssistantTodoDto("1 open quiz to take in Stats Crunch", "Open the study room", "/groups/2/room"),
                new AssistantTodoDto("Next session: IS442 revision on Wed 28 Oct, 12:00 PM", "View session", "/sessions/5"),
                new AssistantTodoDto("6 unread notifications", "Open notifications", "/notifications")), reply.todos());
        verify(applications, never()).listPending(2L, ME);
    }

    @Test
    void saysWhenNothingIsWaiting() {
        when(requests.listIncoming(ME)).thenReturn(List.of());
        when(groups.mine(ME)).thenReturn(List.of());
        when(sessions.mine(ME)).thenReturn(List.of());

        AssistantReplyDto reply = todo.reply(ME, AssistantRequest.unknown());

        assertTrue(reply.text().startsWith("You're all caught up"));
        assertTrue(reply.todos().isEmpty());
    }
}
