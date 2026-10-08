package com.studybuddy.integration;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BuddyHttpTest extends PostgresHttpTest {
    @Test
    void contactIsWithheldUntilAcceptanceAndRemovedImmediatelyOnDisconnect() {
        long alice = student("alice@example.test", "Alice", "private-alice-contact");
        long bob = student("bob@example.test", "Bob", "private-bob-contact");
        student("eve@example.test", "Eve", "private-eve-contact");
        String a = login("alice@example.test"), b = login("bob@example.test"), e = login("eve@example.test");

        var own = expect(call("GET", "/students/" + alice + "/profile", a, null), 200);
        assertEquals("private-alice-contact", own.path("contactNumber").asString());
        assertEquals("SELF", own.path("relationship").path("state").asString());
        privateProfile(bob, a, "STRANGER");
        long request = send(bob, a);
        privateProfile(bob, a, "OUTGOING_PENDING");
        privateProfile(alice, b, "INCOMING_PENDING");
        privateProfile(bob, e, "STRANGER");
        expect(call("POST", "/match-requests/" + request + "/accept", e, null), 403);
        expect(call("POST", "/match-requests/" + request + "/decline", b, null), 200);
        privateProfile(bob, a, "STRANGER");

        request = send(bob, a);
        var accepted = expect(call("POST", "/match-requests/" + request + "/accept", b, null), 200);
        assertEquals("ACCEPTED", accepted.path("status").asString());
        assertNotNull(Instant.parse(accepted.path("respondedAt").asString()));
        var profile = expect(call("GET", "/students/" + bob + "/profile", a, null), 200);
        assertEquals("private-bob-contact", profile.path("contactNumber").asString());
        assertEquals("CONNECTED", profile.path("relationship").path("state").asString());
        long connection = profile.path("relationship").path("connectionId").asLong();
        expect(call("DELETE", "/connections/" + connection, e, null), 403);
        expect(call("DELETE", "/connections/" + connection, a, null), 204);
        privateProfile(bob, a, "STRANGER");
        assertEquals(0, expect(call("GET", "/connections", b, null), 200).size());
        assertEquals(0, expect(call("GET", "/students/me/summary", a, null), 200).path("activeConnections").asLong());
    }

    @Test
    void requestUsesAuthenticatedSenderAndPreservesMatchingContext() {
        long alice = student("alice@example.test", "Alice", "alice-private");
        long bob = student("bob@example.test", "Bob", "bob-private");
        long eve = student("eve@example.test", "Eve", "eve-private");
        String a = login("alice@example.test");
        var sent = expect(call("POST", "/match-requests", a, Map.of("senderId", eve, "receiverId", bob,
                "message", " From matching ", "context", Map.of("origin", "MATCHING", "courseId", course,
                        "studyGoal", "CONCEPT_REVIEW"))), 201);
        assertEquals(alice, sent.path("senderId").asLong());
        assertEquals("MATCHING", sent.path("context").path("origin").asString());
        assertEquals(course, sent.path("context").path("courseId").asLong());
        assertEquals("IS442", sent.path("context").path("courseCode").asString());
        assertNotNull(Instant.parse(sent.path("createdAt").asString()));
        assertFalse(sent.toString().contains("private"));
        var outgoing = expect(call("GET", "/match-requests/outgoing", a, null), 200);
        assertEquals(sent.path("id").asLong(), outgoing.get(0).path("id").asLong());
    }

    @Test
    void concurrentOppositeRequestsCreateOnePendingPairAndOneNotification() throws Exception {
        long alice = student("alice@example.test", "Alice", "a");
        long bob = student("bob@example.test", "Bob", "b");
        String a = login("alice@example.test"), b = login("bob@example.test");
        statuses(race(() -> call("POST", "/match-requests", a, Map.of("receiverId", bob)),
                () -> call("POST", "/match-requests", b, Map.of("receiverId", alice))), 201, 409);
        assertEquals(1, count("select count(*) from match_requests where status='PENDING'"));
        assertEquals(1, count("select count(*) from notifications where type='MATCH_REQUEST_RECEIVED'"));
    }

    @Test
    void concurrentAcceptanceCreatesExactlyOneConnectionAndEvent() throws Exception {
        student("alice@example.test", "Alice", "a");
        long bob = student("bob@example.test", "Bob", "b");
        String a = login("alice@example.test"), b = login("bob@example.test");
        long request = send(bob, a);
        statuses(race(() -> call("POST", "/match-requests/" + request + "/accept", b, null),
                () -> call("POST", "/match-requests/" + request + "/accept", b, null)), 200, 409);
        assertEquals(1, count("select count(*) from connections where ended_at is null"));
        assertEquals(1, count("select count(*) from notifications where type='MATCH_REQUEST_ACCEPTED'"));
    }

    @Test
    void acceptanceAndDeclineCannotBothWin() throws Exception {
        student("alice@example.test", "Alice", "a");
        long bob = student("bob@example.test", "Bob", "b");
        String a = login("alice@example.test"), b = login("bob@example.test");
        long request = send(bob, a);
        statuses(race(() -> call("POST", "/match-requests/" + request + "/accept", b, null),
                () -> call("POST", "/match-requests/" + request + "/decline", b, null)), 200, 409);
        String status = database.queryForObject("select status from match_requests where id=?", String.class, request);
        assertEquals("ACCEPTED".equals(status) ? 1 : 0, count("select count(*) from connections"));
        assertEquals(1, count("select count(*) from notifications where type in ('MATCH_REQUEST_ACCEPTED','MATCH_REQUEST_DECLINED')"));
    }

    @Test
    void notificationsAreRecipientScopedAndReadsArePersistentAndIdempotent() {
        student("alice@example.test", "Alice", "a");
        long bob = student("bob@example.test", "Bob", "b");
        String a = login("alice@example.test"), b = login("bob@example.test");
        send(bob, a);
        long event = expect(call("GET", "/notifications?filter=REQUESTS", b, null), 200).get(0).path("id").asLong();
        assertEquals(1, expect(call("GET", "/notifications/unread-count", b, null), 200).path("count").asLong());
        assertEquals(0, expect(call("GET", "/notifications?filter=GROUPS", b, null), 200).size());
        expect(call("POST", "/notifications/" + event + "/read", a, null), 403);
        assertTrue(expect(call("POST", "/notifications/" + event + "/read", b, null), 200).path("read").asBoolean());
        expect(call("POST", "/notifications/" + event + "/read", b, null), 200);
        assertTrue(database.queryForObject("select read from notifications where id=?", Boolean.class, event));
        expect(call("POST", "/notifications/read-all", b, null), 204);
        assertEquals(0, expect(call("GET", "/notifications/unread-count", b, null), 200).path("count").asLong());
        assertEquals(0, expect(call("GET", "/notifications", a, null), 200).size());
    }

    @Test
    void notificationFailureRollsBackDomainChange() {
        student("alice@example.test", "Alice", "a");
        long bob = student("bob@example.test", "Bob", "b");
        String a = login("alice@example.test");
        database.execute("alter table notifications add constraint test_notification_failure check (false) not valid");
        try {
            expect(call("POST", "/match-requests", a, Map.of("receiverId", bob)), 409);
            assertEquals(0, count("select count(*) from match_requests"));
            assertEquals(0, count("select count(*) from notifications"));
        } finally {
            database.execute("alter table notifications drop constraint test_notification_failure");
        }
    }

    @Test
    void invalidInputsAndMissingAuthenticationReturnSafeProblemDetails() {
        long alice = student("alice@example.test", "Alice", "a");
        long bob = student("bob@example.test", "Bob", "b");
        String a = login("alice@example.test");
        Reply unauthorized = call("GET", "/connections", null, null);
        expect(unauthorized, 401);
        assertTrue(unauthorized.contentType().contains("application/problem+json"));
        assertEquals(401, unauthorized.payload().path("status").asInt());
        assertEquals("/api/connections", unauthorized.payload().path("instance").asString());
        expect(call("POST", "/match-requests", a, Map.of("receiverId", alice)), 409);
        for (Map<String, Object> body : List.<Map<String, Object>>of(Map.of("receiverId", bob, "message", "x".repeat(256)),
                Map.of("receiverId", bob, "context", Map.of("origin", "MATCHING", "courseId", -1)))) {
            Reply failure = call("POST", "/match-requests", a, body);
            expect(failure, 400);
            assertFalse(failure.body().contains("org.hibernate"));
            assertFalse(failure.body().contains("password_hash"));
            assertTrue(failure.payload().has("detail"));
        }
        expect(call("POST", "/match-requests", a, Map.of("receiverId", 999999)), 404);
        assertEquals(0, count("select count(*) from match_requests"));
    }

    private void privateProfile(long subject, String token, String relationship) {
        var reply = call("GET", "/students/" + subject + "/profile", token, null);
        var profile = expect(reply, 200);
        assertFalse(profile.has("contactNumber"), reply.body());
        assertFalse(reply.body().contains("private-"));
        assertEquals(relationship, profile.path("relationship").path("state").asString());
        assertEquals(1, profile.path("availability").size());
    }
}
