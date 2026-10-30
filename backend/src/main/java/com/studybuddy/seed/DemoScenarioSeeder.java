package com.studybuddy.seed;

import com.studybuddy.admin.AdminUserService;
import com.studybuddy.connection.ConnectionDto;
import com.studybuddy.connection.ConnectionService;
import com.studybuddy.matchrequest.MatchRequestContext;
import com.studybuddy.matchrequest.MatchRequestOrigin;
import com.studybuddy.matchrequest.MatchRequestService;
import com.studybuddy.notification.NotificationDto;
import com.studybuddy.notification.NotificationFilter;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.notification.NotificationType;
import com.studybuddy.seed.DemoData.GroupEntry;
import com.studybuddy.seed.DemoData.RequestEntry;
import com.studybuddy.seed.DemoData.RequestOutcome;
import com.studybuddy.studygroup.GroupJoinRequestService;
import com.studybuddy.studygroup.StudyGroupDetails;
import com.studybuddy.studygroup.StudyGroupService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.studybuddy.seed.DemoStudentSeeder.courseIdOf;

/**
 * Replays the demo's history (match requests, connections, study groups and a deactivated
 * account) through the same services the API uses. Every rule is applied and every
 * notification is sent exactly as it would be in live use. Whoever answered a request read its
 * notice first, as they would have in the app, so only requests still open stay unread.
 */
@Component
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoScenarioSeeder {

    private static final String JOIN_MESSAGE = "Hi! Could I join this group?";
    private static final int DAYS_PER_WEEK = 7;

    private final MatchRequestService requests;
    private final ConnectionService connections;
    private final StudyGroupService groups;
    private final GroupJoinRequestService joinRequests;
    private final AdminUserService admin;
    private final NotificationService notifications;
    private final DemoGroupHistory history;
    private final Clock clock;

    public DemoScenarioSeeder(MatchRequestService requests, ConnectionService connections, StudyGroupService groups,
            GroupJoinRequestService joinRequests, AdminUserService admin, NotificationService notifications,
            DemoGroupHistory history, Clock clock) {
        this.requests = requests;
        this.connections = connections;
        this.groups = groups;
        this.joinRequests = joinRequests;
        this.admin = admin;
        this.notifications = notifications;
        this.history = history;
        this.clock = clock;
    }

    /** Replays the history and returns the new groups' ids by name, for the sessions and quizzes that follow. */
    public Map<String, Long> seed(DemoData data, DemoRoster roster, Map<String, Long> courseIds, Long adminId) {
        for (RequestEntry request : data.requests()) {
            replay(request, roster, courseIds);
        }
        Map<String, Long> groupIds = new LinkedHashMap<>();
        for (GroupEntry group : data.groups()) {
            groupIds.put(group.name(), form(group, data, roster, courseIds));
        }
        for (String handle : data.deactivated()) {
            admin.deactivate(roster.id(handle), adminId);
        }
        return groupIds;
    }

    private void replay(RequestEntry entry, DemoRoster roster, Map<String, Long> courseIds) {
        Long sender = roster.id(entry.from());
        Long receiver = roster.id(entry.to());
        MatchRequestContext context = entry.course() == null ? MatchRequestContext.profile()
                : new MatchRequestContext(MatchRequestOrigin.MATCHING, courseIdOf(entry.course(), courseIds), null);
        Long requestId = requests.send(sender, receiver, entry.message(), context).id();
        if (entry.outcome() != RequestOutcome.PENDING && entry.outcome() != RequestOutcome.CANCELLED) {
            readNotices(receiver, NotificationType.MATCH_REQUEST_RECEIVED, requestId);
        }
        switch (entry.outcome()) {
            case PENDING -> { }
            case DECLINED -> requests.decline(requestId, receiver);
            case CANCELLED -> requests.cancel(requestId, sender);
            case CONNECTED -> requests.accept(requestId, receiver);
            case ENDED -> {
                requests.accept(requestId, receiver);
                connections.end(connectionBetween(receiver, sender), receiver);
            }
        }
    }

    /** Marks read the student's unread notices of this type about this request or group. */
    private void readNotices(Long studentId, NotificationType type, Long resourceId) {
        notifications.list(studentId, NotificationFilter.ALL).stream()
                .filter(notice -> !notice.read() && notice.type() == type && resourceId.equals(notice.resourceId()))
                .map(NotificationDto::id)
                .forEach(id -> notifications.markRead(id, studentId));
    }

    private Long connectionBetween(Long studentId, Long otherStudentId) {
        return connections.listActive(studentId).stream()
                .filter(connection -> connection.otherStudentId().equals(otherStudentId))
                .map(ConnectionDto::id).findFirst()
                .orElseThrow(() -> new IllegalStateException("Demo connection was not created"));
    }

    private Long form(GroupEntry entry, DemoData data, DemoRoster roster, Map<String, Long> courseIds) {
        Long leader = roster.id(entry.leader());
        StudyGroupDetails details = new StudyGroupDetails(entry.name(), entry.description(),
                courseIdOf(entry.course(), courseIds), entry.goals(), entry.studyMode(), entry.maxSize(),
                data.groupSlots(entry.meets()));
        Long groupId = groups.create(leader, details).id();
        for (String member : entry.members()) {
            Long requestId = joinRequests.request(groupId, roster.id(member), JOIN_MESSAGE).id();
            joinRequests.accept(groupId, requestId, leader);
        }
        // Read before anyone else applies, so only the still-pending applications stay unread.
        readNotices(leader, NotificationType.GROUP_JOIN_REQUEST_RECEIVED, groupId);
        if (entry.formedWeeksAgo() > 0) {
            history.dateFormation(groupId, clock.instant().minus(Duration.ofDays(DAYS_PER_WEEK * entry.formedWeeksAgo())));
        }
        for (String applicant : entry.applicants()) {
            joinRequests.request(groupId, roster.id(applicant), JOIN_MESSAGE);
        }
        if (entry.closed()) {
            groups.close(groupId, leader);
        }
        return groupId;
    }
}
