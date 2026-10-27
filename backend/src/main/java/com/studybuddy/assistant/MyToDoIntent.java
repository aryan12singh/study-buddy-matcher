package com.studybuddy.assistant;

import com.studybuddy.matchrequest.MatchRequestDto;
import com.studybuddy.matchrequest.MatchRequestService;
import com.studybuddy.matchrequest.MatchRequestStatus;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.quiz.QuizAttemptState;
import com.studybuddy.quiz.QuizDto;
import com.studybuddy.quiz.QuizService;
import com.studybuddy.quiz.QuizStatus;
import com.studybuddy.studygroup.GroupJoinRequestService;
import com.studybuddy.studygroup.StudyGroupService;
import com.studybuddy.studygroup.StudyGroupSummaryDto;
import com.studybuddy.studysession.SessionDto;
import com.studybuddy.studysession.StudySessionService;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * "What do I need to do?": gathers what is waiting for the student from the app's own services, read
 * only: buddy requests to answer, applications to groups they lead, open group quizzes they have not
 * taken, sessions in the coming week and unread notifications. Each item links to where it is dealt
 * with.
 */
@Component
public class MyToDoIntent implements AssistantIntent {
    /** Sessions starting within this long count as coming up. */
    private static final Duration COMING_UP = Duration.ofDays(7);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE d MMM, h:mm a", Locale.ENGLISH);

    private final MatchRequestService requests;
    private final StudyGroupService groups;
    private final GroupJoinRequestService applications;
    private final QuizService quizzes;
    private final StudySessionService sessions;
    private final NotificationService notifications;
    private final AssistantProperties properties;
    private final Clock clock;

    public MyToDoIntent(MatchRequestService requests, StudyGroupService groups, GroupJoinRequestService applications,
            QuizService quizzes, StudySessionService sessions, NotificationService notifications,
            AssistantProperties properties, Clock clock) {
        this.requests = requests;
        this.groups = groups;
        this.applications = applications;
        this.quizzes = quizzes;
        this.sessions = sessions;
        this.notifications = notifications;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public AssistantIntentType type() {
        return AssistantIntentType.MY_TO_DO;
    }

    @Override
    public AssistantReplyDto reply(Long actorId, AssistantRequest request) {
        List<AssistantTodoDto> todos = new ArrayList<>();
        buddyRequests(actorId, todos);
        List<StudyGroupSummaryDto> open = groups.mine(actorId).stream().filter(StudyGroupSummaryDto::active).toList();
        for (StudyGroupSummaryDto group : open) {
            groupApplications(actorId, group, todos);
            groupQuizzes(actorId, group, todos);
        }
        upcomingSessions(actorId, todos);
        long unread = notifications.unreadCount(actorId);
        if (unread > 0) {
            todos.add(new AssistantTodoDto(AssistantWording.count(unread, "unread notification", "unread notifications"),
                    "Open notifications", AppPage.NOTIFICATIONS.pathFor(actorId)));
        }
        String text = todos.isEmpty()
                ? "You're all caught up: no requests, applications, quizzes or sessions are waiting for you."
                : "Here's what's waiting for you:";
        return new AssistantReplyDto(type(), text, List.of(), List.of(), List.of(), todos, null);
    }

    private void buddyRequests(Long actorId, List<AssistantTodoDto> todos) {
        List<MatchRequestDto> pending = requests.listIncoming(actorId).stream()
                .filter(incoming -> incoming.status() == MatchRequestStatus.PENDING).toList();
        if (pending.isEmpty()) return;
        String from = pending.size() == 1 ? " from " + pending.get(0).senderName() : "";
        todos.add(new AssistantTodoDto(AssistantWording.count(pending.size(), "buddy request", "buddy requests")
                + from + " waiting for your reply", "Review requests", AppPage.BUDDY_REQUESTS.pathFor(actorId)));
    }

    private void groupApplications(Long actorId, StudyGroupSummaryDto group, List<AssistantTodoDto> todos) {
        if (!Objects.equals(group.leaderId(), actorId)) return;
        int waiting = applications.listPending(group.id(), actorId).size();
        if (waiting == 0) return;
        todos.add(new AssistantTodoDto(AssistantWording.count(waiting, "application", "applications") + " to join "
                + group.name(), "Review applications", "/groups/" + group.id() + "/manage"));
    }

    private void groupQuizzes(Long actorId, StudyGroupSummaryDto group, List<AssistantTodoDto> todos) {
        List<QuizDto> untaken = quizzes.list(group.id(), actorId).stream()
                .filter(quiz -> quiz.status() == QuizStatus.OPEN && quiz.myAttempt() != QuizAttemptState.SUBMITTED).toList();
        if (untaken.isEmpty()) return;
        todos.add(new AssistantTodoDto(AssistantWording.count(untaken.size(), "open quiz", "open quizzes") + " to take in "
                + group.name(), "Open the study room", "/groups/" + group.id() + "/room"));
    }

    private void upcomingSessions(Long actorId, List<AssistantTodoDto> todos) {
        var horizon = clock.instant().plus(COMING_UP);
        List<SessionDto> soon = sessions.mine(actorId).stream()
                .filter(session -> session.nextOccurrence().startAt().isBefore(horizon)).toList();
        if (soon.isEmpty()) return;
        SessionDto next = soon.get(0);
        String when = WHEN.format(next.nextOccurrence().startAt().atZone(ZoneId.of(properties.timeZone())));
        String more = soon.size() == 1 ? "" : " (" + AssistantWording.count(soon.size(), "session", "sessions") + " this week)";
        todos.add(new AssistantTodoDto("Next session: " + next.title() + " on " + when + more,
                "View session", "/sessions/" + next.id()));
    }
}
