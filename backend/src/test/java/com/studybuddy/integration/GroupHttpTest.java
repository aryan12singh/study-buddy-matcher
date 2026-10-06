package com.studybuddy.integration;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GroupHttpTest extends PostgresHttpTest {
    @Test
    void groupLifecycleSupportsFullDetailsApplicationsRemovalAndClosure() {
        long leader = student("leader@example.test", "Leader", "leader-secret-contact");
        long member = student("member@example.test", "Member", "member-secret-contact");
        student("pending@example.test", "Applicant", "applicant-secret-contact");
        String l = login("leader@example.test"), m = login("member@example.test"), p = login("pending@example.test");
        long group = group(l, 3);
        var detail = expect(call("GET", "/groups/" + group, l, null), 200);
        assertEquals(1, detail.path("memberCount").asLong());
        assertTrue(detail.path("viewer").path("leader").asBoolean());
        assertEquals(leader, detail.path("members").get(0).path("studentId").asLong());
        assertEquals(1, detail.path("availability").size());
        assertEquals(1, expect(call("GET", "/groups/mine", l, null), 200).size());
        assertEquals(1, expect(call("GET", "/groups?courseId=" + course + "&studyMode=IN_PERSON&studyGoal=CONCEPT_REVIEW", m, null), 200).size());
        assertEquals(0, expect(call("GET", "/groups?studyMode=ONLINE", m, null), 200).size());
        long request = apply(group, m);
        long pending = apply(group, p);
        assertEquals(1, expect(call("GET", "/group-join-requests/mine", m, null), 200).size());
        assertEquals(2, expect(call("GET", "/groups/" + group + "/join-requests", l, null), 200).size());
        expect(call("GET", "/groups/" + group + "/join-requests", m, null), 403);
        expect(approve(group, request, m), 403);
        expect(approve(group, request, l), 200);
        assertEquals(2, expect(call("GET", "/groups/" + group, m, null), 200).path("memberCount").asLong());
        assertTrue(expect(call("GET", "/groups/" + group, m, null), 200).path("viewer").path("member").asBoolean());
        var peer = call("GET", "/students/" + leader + "/profile", m, null);
        expect(peer, 200);
        assertFalse(peer.body().contains("leader-secret-contact"));
        assertFalse(peer.payload().has("contactNumber"), "Group membership does not grant buddy contact access");
        expect(call("PUT", "/groups/" + group, m, groupDetails("Not the leader", 3)), 403);
        expect(call("DELETE", "/groups/" + group + "/members/" + leader, l, null), 409);
        expect(call("DELETE", "/groups/" + group + "/members/" + member, l, null), 204);
        assertEquals(0, expect(call("GET", "/groups/mine", m, null), 200).size());
        expect(call("PUT", "/groups/" + group, l, groupDetails("Updated group", 2)), 200);
        expect(call("POST", "/groups/" + group + "/close", l, null), 200);
        assertEquals("REJECTED", database.queryForObject("select status from group_join_requests where id=?", String.class, pending));
        assertEquals(0, expect(call("GET", "/groups", m, null), 200).size());
        assertFalse(expect(call("GET", "/groups/" + group, l, null), 200).path("active").asBoolean());
        expect(approve(group, pending, l), 409);
        expect(call("PUT", "/groups/" + group, l, groupDetails("Reopen", 2)), 409);
        assertEquals(1, count("select count(*) from notifications where type='GROUP_JOIN_REQUEST_REJECTED'"));
    }

    @Test
    void concurrentApprovalsCannotExceedTheLastAvailablePlace() throws Exception {
        student("leader@example.test", "Leader", "l");
        student("one@example.test", "One", "1");
        student("two@example.test", "Two", "2");
        String l = login("leader@example.test"), a = login("one@example.test"), b = login("two@example.test");
        long group = group(l, 2), left = apply(group, a), right = apply(group, b);
        statuses(race(() -> approve(group, left, l), () -> approve(group, right, l)), 200, 409);
        assertEquals(2, count("select count(*) from group_memberships where study_group_id=?", group));
        assertEquals(1, count("select count(*) from group_join_requests where status='ACCEPTED'"));
        assertEquals(1, count("select count(*) from group_join_requests where status='PENDING'"));
        assertEquals(1, count("select count(*) from notifications where type='GROUP_JOIN_REQUEST_ACCEPTED'"));
    }

    @Test
    void capacityReductionAndApprovalRemainConsistent() throws Exception {
        student("leader@example.test", "Leader", "l");
        student("member@example.test", "Member", "m");
        student("next@example.test", "Next", "n");
        String l = login("leader@example.test"), m = login("member@example.test"), n = login("next@example.test");
        long group = group(l, 3);
        expect(approve(group, apply(group, m), l), 200);
        long request = apply(group, n);
        statuses(race(() -> call("PUT", "/groups/" + group, l, groupDetails("Capacity edit", 2)),
                () -> approve(group, request, l)), 200, 409);
        long members = count("select count(*) from group_memberships where study_group_id=?", group);
        long capacity = count("select max_group_size from study_groups where id=?", group);
        assertTrue(members <= capacity);
    }

    @Test
    void closureAndApprovalCannotLeaveAnActiveGroupOrPendingApplication() throws Exception {
        student("leader@example.test", "Leader", "l");
        student("member@example.test", "Member", "m");
        String l = login("leader@example.test"), m = login("member@example.test");
        long group = group(l, 2), request = apply(group, m);
        List<Reply> replies = race(() -> call("POST", "/groups/" + group + "/close", l, null),
                () -> approve(group, request, l));
        assertEquals(200, replies.get(0).status());
        assertTrue(List.of(200, 409).contains(replies.get(1).status()));
        assertFalse(database.queryForObject("select active from study_groups where id=?", Boolean.class, group));
        assertEquals(0, count("select count(*) from group_join_requests where status='PENDING'"));
        assertEquals(0, expect(call("GET", "/students/me/summary", m, null), 200).path("acceptedGroups").asLong());
    }

    @Test
    void duplicateApplicationAndMismatchedGroupRequestAreRejected() throws Exception {
        student("leader@example.test", "Leader", "l");
        student("member@example.test", "Member", "m");
        String l = login("leader@example.test"), m = login("member@example.test");
        long group = group(l, 3), other = group(l, 3);
        statuses(race(() -> call("POST", "/groups/" + group + "/join-requests", m, Map.of()),
                () -> call("POST", "/groups/" + group + "/join-requests", m, Map.of())), 201, 409);
        long request = count("select id from group_join_requests where study_group_id=?", group);
        expect(approve(other, request, l), 404);
        assertEquals(1, count("select count(*) from group_memberships where study_group_id=?", group));
        expect(call("POST", "/groups/" + group + "/join-requests/" + request + "/reject", l, null), 200);
        apply(group, m);
        assertEquals(2, expect(call("GET", "/group-join-requests/mine", m, null), 200).size());
    }

    @Test
    void invalidSlotsAndCapacityProduceFieldErrorsAndNoPartialRows() {
        student("leader@example.test", "Leader", "l");
        String l = login("leader@example.test");
        for (Map<String, Object> slot : List.<Map<String, Object>>of(
                Map.of("dayOfWeek", "MONDAY", "startTime", "11:00", "endTime", "09:00"),
                Map.of("dayOfWeek", "MONDAY", "startTime", "09:00:30", "endTime", "11:00"))) {
            Map<String, Object> body = new HashMap<>(groupDetails("Invalid schedule", 2));
            body.put("availability", List.of(slot));
            Reply error = call("POST", "/groups", l, body);
            expect(error, 400);
            assertTrue(error.payload().path("fieldErrors").has("availability"));
        }
        expect(call("POST", "/groups", l, groupDetails("Invalid capacity", 1)), 400);
        assertEquals(0, count("select count(*) from study_groups"));
        assertEquals(0, count("select count(*) from group_memberships"));
        Map<String, Object> optional = new HashMap<>(groupDetails("Optional fields", 2));
        optional.remove("studyGoals");
        optional.remove("preferredStudyMode");
        optional.remove("availability");
        assertEquals(0, expect(call("POST", "/groups", l, optional), 201).path("availability").size());
    }

    @Test
    void notificationFailureRollsBackAcceptanceAndMembershipTogether() {
        student("leader@example.test", "Leader", "l");
        student("member@example.test", "Member", "m");
        String l = login("leader@example.test"), m = login("member@example.test");
        long group = group(l, 2), request = apply(group, m);
        database.execute("alter table notifications add constraint test_notification_failure check (false) not valid");
        try {
            expect(approve(group, request, l), 409);
            assertEquals("PENDING", database.queryForObject("select status from group_join_requests where id=?", String.class, request));
            assertEquals(1, count("select count(*) from group_memberships where study_group_id=?", group));
        } finally {
            database.execute("alter table notifications drop constraint test_notification_failure");
        }
    }
}
