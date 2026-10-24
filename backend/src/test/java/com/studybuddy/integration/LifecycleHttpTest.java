package com.studybuddy.integration;

import java.util.UUID;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** A student can take back their own pending request or application, and leave a group they joined. */
class LifecycleHttpTest extends PostgresHttpTest {

    @Test
    void senderCancelsAPendingRequestWhichFreesThePairForANewOne() {
        long alice = student("alice@example.test", "Alice", "+6591234567");
        long bob = student("bob@example.test", "Bob", "+6591234568");
        String a = login("alice@example.test"), b = login("bob@example.test");
        long request = send(bob, a);

        expect(call("POST", "/match-requests/" + request + "/cancel", b, null), 403);
        JsonNode cancelled = expect(call("POST", "/match-requests/" + request + "/cancel", a, null), 200);
        assertEquals("CANCELLED", cancelled.path("status").asString());
        assertEquals(1, count("select count(*) from notifications where recipient_id=? and type='MATCH_REQUEST_CANCELLED'", bob));
        expect(call("POST", "/match-requests/" + request + "/accept", b, null), 409);
        expect(call("POST", "/match-requests/" + request + "/cancel", a, null), 409);

        // The pending pair is free again, so either student can send a fresh request.
        send(alice, b);
    }

    @Test
    void applicantWithdrawsAPendingApplicationAndCanApplyAgain() {
        long leader = student("leader@example.test", "Leader", "+6591234567");
        student("applicant@example.test", "Applicant", "+6591234568");
        student("other@example.test", "Other", "+6591234569");
        String l = login("leader@example.test"), p = login("applicant@example.test"), o = login("other@example.test");
        long group = group(l, 3);
        long application = apply(group, p);

        expect(call("POST", "/group-join-requests/" + application + "/withdraw", o, null), 404);
        JsonNode withdrawn = expect(call("POST", "/group-join-requests/" + application + "/withdraw", p, null), 200);
        assertEquals("WITHDRAWN", withdrawn.path("status").asString());
        assertEquals(1, count("select count(*) from notifications where recipient_id=? and type='GROUP_JOIN_REQUEST_WITHDRAWN'", leader));
        assertEquals(0, expect(call("GET", "/groups/" + group + "/join-requests", l, null), 200).size());
        expect(approve(group, application, l), 409);
        expect(call("POST", "/group-join-requests/" + application + "/withdraw", p, null), 409);

        apply(group, p);
    }

    @Test
    void memberLeavesAnOpenGroupButTheLeaderMustCloseItInstead() {
        long leader = student("leader@example.test", "Leader", "+6591234567");
        student("member@example.test", "Member", "+6591234568");
        student("stranger@example.test", "Stranger", "+6591234569");
        String l = login("leader@example.test"), m = login("member@example.test"), s = login("stranger@example.test");
        long group = group(l, 3);
        expect(approve(group, apply(group, m), l), 200);

        expect(call("POST", "/groups/" + group + "/leave", l, null), 409);
        expect(call("POST", "/groups/" + group + "/leave", s, null), 409);
        expect(call("POST", "/groups/" + group + "/room/join", m, Map.of("clientId", UUID.randomUUID())), 200);
        expect(call("POST", "/groups/" + group + "/leave", m, null), 204);
        assertEquals(0, count("select count(*) from room_presences"), "Leaving also leaves the study room");
        assertEquals(1, expect(call("GET", "/groups/" + group, l, null), 200).path("memberCount").asLong());
        assertFalse(expect(call("GET", "/groups/" + group, m, null), 200).path("viewer").path("member").asBoolean());
        assertEquals(1, count("select count(*) from notifications where recipient_id=? and type='GROUP_MEMBER_LEFT'", leader));
        expect(call("POST", "/groups/" + group + "/leave", m, null), 409);

        // A former member can apply again, and a closed group keeps its members as history.
        expect(approve(group, apply(group, m), l), 200);
        expect(call("POST", "/groups/" + group + "/close", l, null), 200);
        expect(call("POST", "/groups/" + group + "/leave", m, null), 409);
    }

    @Test
    void leavingRevokesRoomAndSessionAccessWithoutAffectingTheRemainingMembers() {
        long leader = student("leader@example.test", "Leader", "+6591234567");
        long member = student("member@example.test", "Member", "+6591234568");
        String l = login("leader@example.test"), m = login("member@example.test");
        long group = group(l, 3);
        expect(approve(group, apply(group, m), l), 200);
        UUID tab = UUID.randomUUID();
        expect(call("POST", "/groups/" + group + "/room/join", m, Map.of("clientId", tab)), 200);
        LocalDateTime start = LocalDateTime.now(ZoneId.of("Asia/Singapore")).plusDays(1).withSecond(0).withNano(0);
        long session = expect(call("POST", "/groups/" + group + "/sessions", l, Map.of("title", "Revision",
            "startLocal", start.toString(), "endLocal", start.plusHours(1).toString(), "timeZone", "Asia/Singapore", "occurrenceCount", 1)), 201).path("id").asLong();
        expect(call("GET", "/sessions/" + session + "/calendar", m, null), 200);

        expect(call("POST", "/groups/" + group + "/leave", m, null), 204);

        assertEquals(0, count("select count(*) from room_presences where student_id=?", member));
        expect(call("GET", "/groups/" + group + "/room", m, null), 403);
        expect(call("POST", "/groups/" + group + "/room/join", m, Map.of("clientId", tab)), 403);
        expect(call("PUT", "/groups/" + group + "/room/presence", m, Map.of("clientId", tab, "presence", "FOCUS")), 403);
        expect(call("GET", "/groups/" + group + "/sessions", m, null), 403);
        expect(call("GET", "/sessions/" + session, m, null), 403);
        expect(call("GET", "/sessions/" + session + "/calendar", m, null), 403);
        assertEquals(0, expect(call("GET", "/sessions/mine", m, null), 200).size());
        expect(call("GET", "/groups/" + group + "/room", l, null), 200);
        expect(call("GET", "/sessions/" + session, l, null), 200);
        expect(call("GET", "/sessions/" + session + "/calendar", l, null), 200);
        assertEquals("SCHEDULED", database.queryForObject("select status from study_sessions where id=?", String.class, session));
        assertEquals(1, count("select count(*) from group_memberships where student_id=? and study_group_id=?", leader, group));
    }

    @Test
    void aFailedLeaveNotificationRollsBackMembershipAndRoomRevocation() {
        student("leader@example.test", "Leader", "+6591234567");
        long member = student("member@example.test", "Member", "+6591234568");
        String l = login("leader@example.test"), m = login("member@example.test");
        long group = group(l, 3);
        expect(approve(group, apply(group, m), l), 200);
        expect(call("POST", "/groups/" + group + "/room/join", m, Map.of("clientId", UUID.randomUUID())), 200);
        database.execute("alter table notifications add constraint test_notification_failure check(false) not valid");

        expect(call("POST", "/groups/" + group + "/leave", m, null), 409);

        assertEquals(1, count("select count(*) from group_memberships where student_id=? and study_group_id=?", member, group));
        assertEquals(1, count("select count(*) from room_presences where student_id=?", member));
        assertEquals(0, count("select count(*) from notifications where type='GROUP_MEMBER_LEFT'"));
        expect(call("GET", "/groups/" + group + "/room", m, null), 200);
    }

    @Test
    void withdrawalAndLeaveEndpointsRejectAnonymousAndAdministratorActors() {
        student("leader@example.test", "Leader", "+6591234567");
        long applicant = student("applicant@example.test", "Applicant", "+6591234568");
        account("admin@example.test", "ADMIN");
        String l = login("leader@example.test"), p = login("applicant@example.test"), admin = login("admin@example.test");
        long group = group(l, 3), application = apply(group, p), request = send(applicant, l);
        for (String path : new String[]{"/match-requests/" + request + "/cancel", "/group-join-requests/" + application + "/withdraw", "/groups/" + group + "/leave"}) {
            expect(call("POST", path, null, null), 401);
            expect(call("POST", path, admin, null), 403);
        }
        assertEquals("PENDING", database.queryForObject("select status from match_requests where id=?", String.class, request));
        assertEquals("PENDING", database.queryForObject("select status from group_join_requests where id=?", String.class, application));
    }
}
