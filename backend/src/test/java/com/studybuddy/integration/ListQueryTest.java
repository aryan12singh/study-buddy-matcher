package com.studybuddy.integration;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Real HTTP/JPA query counts catch list work that unit mocks cannot measure. */
class ListQueryTest extends PostgresHttpTest {
    @Autowired private EntityManagerFactory entityManagers;

    @Test
    void browseAndMineQueriesDoNotGrowWithTheNumberOfGroups() {
        long viewer = student("viewer@example.test", "Viewer", "Viewer private contact");
        long leader = student("leader@example.test", "Leader", "Leader private contact");
        String token = login("viewer@example.test");
        insertGroup("Own initial group", viewer);
        insertGroup("Other initial group", leader);
        var smallBrowse = measure("/groups", token);
        var smallMine = measure("/groups/mine", token);
        for (int index = 0; index < 12; index++) {
            insertGroup("Own group " + index, viewer);
            insertGroup("Other group " + index, leader);
        }

        var largeBrowse = measure("/groups", token);
        var largeMine = measure("/groups/mine", token);

        assertEquals(26, largeBrowse.payload().size());
        assertEquals(13, largeMine.payload().size());
        for (JsonNode group : largeBrowse.payload()) {
            assertEquals(1, group.path("memberCount").asLong());
            assertEquals(2, group.path("studyGoals").size());
            assertEquals("IS442", group.path("courseCode").asString());
        }
        assertFalse(largeBrowse.payload().toString().contains("private contact"));
        assertEquals(smallBrowse.queries(), largeBrowse.queries(), "Browse query count must not grow from 2 to 26 groups");
        assertEquals(smallMine.queries(), largeMine.queries(), "Mine query count must not grow from 1 to 13 groups");
        System.out.printf("Group list queries: browse %d/%d; mine %d/%d%n",
                smallBrowse.queries(), largeBrowse.queries(), smallMine.queries(), largeMine.queries());
    }

    @Test
    void notificationQueriesDoNotGrowWithDistinctReferencedRequests() {
        student("sender@example.test", "Sender", "Sender private contact");
        long receiver = student("receiver@example.test", "Receiver", "Receiver private contact");
        String senderToken = login("sender@example.test"), receiverToken = login("receiver@example.test");
        long first = send(receiver, senderToken);
        var small = measure("/notifications?filter=REQUESTS", receiverToken);
        expect(call("POST", "/match-requests/" + first + "/decline", receiverToken, null), 200);
        for (int index = 0; index < 24; index++) {
            long request = send(receiver, senderToken);
            expect(call("POST", "/match-requests/" + request + "/decline", receiverToken, null), 200);
        }

        var large = measure("/notifications?filter=REQUESTS", receiverToken);

        assertEquals(25, large.payload().size());
        for (JsonNode notification : large.payload()) {
            assertEquals("INCOMING", notification.path("requestDirection").asString());
        }
        assertFalse(large.payload().toString().contains("private contact"));
        assertEquals(small.queries(), large.queries(), "Notification query count must not grow from 1 to 25 referenced requests");
        System.out.printf("Notification list queries: %d/%d%n", small.queries(), large.queries());
    }

    @Test
    void groupListsPreserveLatestApplicationMembershipAndClosedHistory() {
        long viewer = student("viewer@example.test", "Viewer", "Viewer private contact");
        long leader = student("leader@example.test", "Leader", "Leader private contact");
        String viewerToken = login("viewer@example.test"), leaderToken = login("leader@example.test");
        long pendingGroup = group(leaderToken, 4), acceptedGroup = group(leaderToken, 4), closedGroup = group(viewerToken, 4);
        long rejected = apply(pendingGroup, viewerToken);
        expect(call("POST", "/groups/" + pendingGroup + "/join-requests/" + rejected + "/reject", leaderToken, null), 200);
        long pending = apply(pendingGroup, viewerToken);
        database.update("update group_join_requests set created_at=timestamp with time zone '2026-10-01 00:00:00+00' where study_group_id=?", pendingGroup);
        long accepted = apply(acceptedGroup, viewerToken);
        expect(approve(acceptedGroup, accepted, leaderToken), 200);
        expect(call("POST", "/groups/" + closedGroup + "/close", viewerToken, null), 200);

        JsonNode browse = expect(call("GET", "/groups", viewerToken, null), 200);
        JsonNode mine = expect(call("GET", "/groups/mine", viewerToken, null), 200);
        assertEquals(2, browse.size());
        assertEquals(2, mine.size());
        JsonNode application = findGroup(browse, pendingGroup);
        assertEquals(pending, application.path("viewer").path("requestId").asLong());
        assertEquals("PENDING", application.path("viewer").path("requestStatus").asString());
        assertFalse(application.path("viewer").path("member").asBoolean());
        JsonNode membership = findGroup(mine, acceptedGroup);
        assertEquals(2, membership.path("memberCount").asLong());
        assertTrue(membership.path("viewer").path("member").asBoolean());
        assertFalse(membership.path("viewer").path("leader").asBoolean());
        assertEquals(accepted, membership.path("viewer").path("requestId").asLong());
        assertEquals("ACCEPTED", membership.path("viewer").path("requestStatus").asString());
        JsonNode history = findGroup(mine, closedGroup);
        assertFalse(history.path("active").asBoolean());
        assertTrue(history.path("viewer").path("leader").asBoolean());
        assertTrue(history.path("viewer").path("member").asBoolean());
    }

    private long insertGroup(String name, long leader) {
        long id = database.queryForObject("insert into study_groups(name,course_id,leader_id,max_group_size,active,created_at) values (?,?,?,4,true,now()) returning id",
                Long.class, name, course, leader);
        database.update("insert into group_memberships(study_group_id,student_id,joined_at) values (?,?,now())", id, leader);
        database.update("insert into study_group_goals(study_group_id,study_goal) values (?,'CONCEPT_REVIEW'),(?,'PROBLEM_SOLVING')", id, id);
        return id;
    }

    private MeasuredList measure(String path, String token) {
        var statistics = entityManagers.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        statistics.setStatisticsEnabled(true);
        try {
            JsonNode payload = expect(call("GET", path, token, null), 200);
            return new MeasuredList(payload, statistics.getPrepareStatementCount());
        } finally {
            statistics.setStatisticsEnabled(false);
            statistics.clear();
        }
    }

    private static JsonNode findGroup(JsonNode groups, long id) {
        for (JsonNode group : groups) if (group.path("id").asLong() == id) return group;
        throw new AssertionError("Missing group " + id);
    }

    private record MeasuredList(JsonNode payload, long queries) { }
}
