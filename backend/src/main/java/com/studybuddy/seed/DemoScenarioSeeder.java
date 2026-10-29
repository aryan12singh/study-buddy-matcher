package com.studybuddy.seed;

import com.studybuddy.admin.AdminUserService;
import com.studybuddy.connection.ConnectionDto;
import com.studybuddy.connection.ConnectionService;
import com.studybuddy.matchrequest.MatchRequestContext;
import com.studybuddy.matchrequest.MatchRequestOrigin;
import com.studybuddy.matchrequest.MatchRequestService;
import com.studybuddy.seed.DemoData.GroupEntry;
import com.studybuddy.seed.DemoData.RequestEntry;
import com.studybuddy.seed.DemoData.RequestOutcome;
import com.studybuddy.studygroup.GroupJoinRequestService;
import com.studybuddy.studygroup.StudyGroupDetails;
import com.studybuddy.studygroup.StudyGroupService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.studybuddy.seed.DemoStudentSeeder.courseIdOf;

/**
 * Replays the demo's history (match requests, connections, study groups and a deactivated
 * account) through the same services the API uses. Every rule is applied and every
 * notification is sent exactly as it would be in live use.
 */
@Component
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoScenarioSeeder {

    private static final String JOIN_MESSAGE = "Hi! Could I join this group?";

    private final MatchRequestService requests;
    private final ConnectionService connections;
    private final StudyGroupService groups;
    private final GroupJoinRequestService joinRequests;
    private final AdminUserService admin;

    public DemoScenarioSeeder(MatchRequestService requests, ConnectionService connections, StudyGroupService groups,
            GroupJoinRequestService joinRequests, AdminUserService admin) {
        this.requests = requests;
        this.connections = connections;
        this.groups = groups;
        this.joinRequests = joinRequests;
        this.admin = admin;
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
        if (entry.outcome() == RequestOutcome.PENDING) {
            return;
        }
        requests.accept(requestId, receiver);
        if (entry.outcome() == RequestOutcome.ENDED) {
            connections.end(connectionBetween(receiver, sender), receiver);
        }
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
        for (String applicant : entry.applicants()) {
            joinRequests.request(groupId, roster.id(applicant), JOIN_MESSAGE);
        }
        if (entry.closed()) {
            groups.close(groupId, leader);
        }
        return groupId;
    }
}
