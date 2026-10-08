package com.studybuddy.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AccountHttpTest extends PostgresHttpTest {
    @Test
    void adminCreatesAndEditsStudentWithoutReceivingContactOrPassword() {
        account("admin@example.test", "ADMIN");
        student("existing@example.test", "Existing", "existing-private");
        String admin = login("admin@example.test");
        Map<String, Object> create = studentDetails(" MixedCase@Example.Test ", "Initial name");
        var created = expect(call("POST", "/admin/users", admin, create), 201);
        long id = created.path("account").path("id").asLong();
        assertEquals("mixedcase@example.test", created.path("account").path("email").asString());
        assertTrue(created.path("account").path("lastLoginAt").isNull());
        assertEquals(0, created.path("usage").path("activeConnections").asLong());
        assertEquals("Initial name", created.path("profile").path("name").asString());
        assertPrivateAdminPayload(created.toString());
        Map<String, Object> update = new HashMap<>(create);
        update.remove("contactNumber");
        update.put("name", "Updated name");
        update.put("email", "mixedcase@example.test");
        update.put("role", "ADMIN");
        var edited = expect(call("PUT", "/admin/users/" + id, admin, update), 200);
        assertEquals("STUDENT", edited.path("account").path("role").asString());
        assertEquals("Updated name", edited.path("profile").path("name").asString());
        assertEquals("private-created-contact", database.queryForObject("select contact_number from students where id=?", String.class, id));
        update.put("contactNumber", "replacement-private-contact");
        assertPrivateAdminPayload(expect(call("PUT", "/admin/users/" + id, admin, update), 200).toString());
        assertEquals("replacement-private-contact", database.queryForObject("select contact_number from students where id=?", String.class, id));
        assertEquals(1, expect(call("GET", "/admin/users?role=STUDENT&active=true&search=Updated", admin, null), 200).size());
        expect(call("POST", "/admin/users", admin, studentDetails("MIXEDCASE@example.test", "Duplicate")), 409);
        assertEquals(3, count("select count(*) from users"));
        expect(call("GET", "/admin/users", login("existing@example.test"), null), 403);
    }

    @Test
    void registrationCannotElevateRoleAndFailedLoginDoesNotChangeLastLogin() {
        Map<String, Object> registration = studentDetails("New@Example.Test", "New student");
        registration.put("role", "ADMIN");
        var registered = expect(call("POST", "/auth/register", null, registration), 201);
        assertEquals("STUDENT", registered.path("account").path("role").asString());
        long id = registered.path("account").path("id").asLong();
        var firstLogin = database.queryForObject("select last_login_at from users where id=?", java.time.OffsetDateTime.class, id);
        expect(call("POST", "/auth/login", null, Map.of("email", "new@example.test", "password", "wrong-password")), 401);
        assertEquals(firstLogin, database.queryForObject("select last_login_at from users where id=?", java.time.OffsetDateTime.class, id));
        var login = expect(call("POST", "/auth/login", null, Map.of("email", " NEW@example.test ", "password", "test-password-only")), 200);
        assertEquals(id, login.path("account").path("id").asLong());
        assertEquals(1, count("select count(*) from users where id=? and last_login_at is not null", id));
        expect(call("GET", "/auth/me", login.path("token").asString(), null), 200);
    }

    @Test
    void malformedExpiredWrongRoleAndRevokedTokensAreRejected() {
        long student = student("student@example.test", "Student", "private");
        String valid = login("student@example.test");
        expect(call("GET", "/auth/me", valid, null), 200);
        expect(call("GET", "/auth/me", "not-a-token", null), 401);
        var key = Keys.hmacShaKeyFor(environment.getRequiredProperty("app.auth.secret").getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder().subject(Long.toString(student)).claim("role", "STUDENT").claim("version", 0)
                .expiration(Date.from(Instant.now().minusSeconds(60))).signWith(key).compact();
        String wrongRole = Jwts.builder().subject(Long.toString(student)).claim("role", "ADMIN").claim("version", 0)
                .expiration(Date.from(Instant.now().plusSeconds(60))).signWith(key).compact();
        String missingVersion = Jwts.builder().subject(Long.toString(student)).claim("role", "STUDENT")
                .expiration(Date.from(Instant.now().plusSeconds(60))).signWith(key).compact();
        for (String invalid : List.of(expired, wrongRole, missingVersion)) {
            expect(call("GET", "/auth/me", invalid, null), 401);
        }
        database.update("update users set token_version=token_version+1 where id=?", student);
        expect(call("GET", "/auth/me", valid, null), 401);
    }

    @Test
    void deactivationCleansRelationsAndReactivationRequiresFreshLogin() {
        account("admin@example.test", "ADMIN");
        long target = student("target@example.test", "Target", "target-private");
        long buddy = student("buddy@example.test", "Buddy", "buddy-private");
        student("pending@example.test", "Pending", "pending-private");
        student("leader@example.test", "Other leader", "leader-private");
        String admin = login("admin@example.test"), t = login("target@example.test"), b = login("buddy@example.test"),
                p = login("pending@example.test"), l = login("leader@example.test");
        expect(call("POST", "/match-requests/" + send(buddy, t) + "/accept", b, null), 200);
        send(target, p);
        long led = group(t, 3);
        expect(approve(led, apply(led, b), t), 200);
        apply(led, p);
        long other = group(l, 3);
        expect(approve(other, apply(other, t), l), 200);
        long ownPending = group(l, 3);
        apply(ownPending, t);

        var usage = expect(call("GET", "/admin/users/" + target, admin, null), 200).path("usage");
        assertEquals(1, usage.path("activeConnections").asLong());
        assertEquals(2, usage.path("acceptedGroups").asLong());
        var deactivated = expect(call("POST", "/admin/users/" + target + "/deactivate", admin, null), 200);
        assertFalse(deactivated.path("account").path("active").asBoolean());
        assertEquals(0, count("select count(*) from connections where ended_at is null"));
        assertEquals(0, count("select count(*) from match_requests where status='PENDING'"));
        assertEquals(0, count("select count(*) from group_join_requests where status='PENDING'"));
        // Leaves the open group but stays the leader-member of the group it led, which is now closed history.
        assertEquals(0, count("select count(*) from group_memberships where study_group_id=? and student_id=?", other, target));
        assertEquals(1, count("select count(*) from group_memberships where study_group_id=? and student_id=?", led, target));
        assertFalse(database.queryForObject("select active from study_groups where id=?", Boolean.class, led));
        assertTrue(database.queryForObject("select active from study_groups where id=?", Boolean.class, other));
        expect(call("GET", "/connections", t, null), 401);
        assertEquals("ACCOUNT_DEACTIVATED", expect(attemptLogin("target@example.test", true), 403).path("code").asString());
        // A wrong password reveals nothing about the account's state
        assertEquals("INVALID_CREDENTIALS", expect(attemptLogin("target@example.test", false), 401).path("code").asString());
        expect(call("GET", "/students/" + target + "/profile", b, null), 404);
        expect(call("POST", "/admin/users", admin, studentDetails("TARGET@example.test", "Reserved email")), 409);
        expect(call("POST", "/admin/users/" + target + "/reactivate", admin, null), 200);
        expect(call("GET", "/auth/me", t, null), 401);
        String fresh = login("target@example.test");
        assertEquals(0, expect(call("GET", "/connections", fresh, null), 200).size());
        assertEquals(0, expect(call("GET", "/students/me/summary", fresh, null), 200).path("acceptedGroups").asLong());
        assertFalse(expect(call("GET", "/students/" + target + "/profile", b, null), 200).has("contactNumber"));
    }

    @Test
    void permanentDeletionRemovesDependenciesAndResourceLinksButPreservesOtherGroups() {
        account("admin@example.test", "ADMIN");
        long target = student("target@example.test", "Target", "target-private");
        long buddy = student("buddy@example.test", "Buddy", "buddy-private");
        student("applicant@example.test", "Applicant", "applicant-private");
        student("leader@example.test", "Other leader", "leader-private");
        String admin = login("admin@example.test"), t = login("target@example.test"), b = login("buddy@example.test"),
                p = login("applicant@example.test"), l = login("leader@example.test");
        long request = send(buddy, t);
        expect(call("POST", "/match-requests/" + request + "/accept", b, null), 200);
        long led = group(t, 3);
        expect(approve(led, apply(led, b), t), 200);
        apply(led, p);
        long other = group(l, 3);
        expect(approve(other, apply(other, t), l), 200);
        expect(call("DELETE", "/admin/users/" + target, admin, null), 204);
        assertEquals(0, count("select count(*) from users where id=?", target));
        assertEquals(0, count("select count(*) from students where id=?", target));
        assertEquals(0, count("select count(*) from match_requests"));
        assertEquals(0, count("select count(*) from connections"));
        assertEquals(0, count("select count(*) from study_groups where id=?", led));
        assertEquals(0, count("select count(*) from group_memberships where student_id=?", target));
        assertEquals(0, count("select count(*) from student_courses where student_id=?", target));
        assertEquals(0, count("select count(*) from availability_slots where student_id=?", target));
        assertEquals(0, count("select count(*) from notifications where recipient_id=?", target));
        assertEquals(0, count("select count(*) from notifications where resource_type='GROUP' and resource_id=?", led));
        assertTrue(database.queryForObject("select active from study_groups where id=?", Boolean.class, other));
        var notifications = expect(call("GET", "/notifications", b, null), 200);
        assertFalse(notifications.toString().contains("target-private"));
        assertFalse(notifications.toString().contains("Target"), "Deleted identity must not survive in event copy");
        expect(call("GET", "/auth/me", t, null), 401);
        expect(call("GET", "/students/" + target + "/profile", b, null), 404);
        expect(call("POST", "/admin/users", admin, studentDetails("target@example.test", "Replacement")), 201);
    }

    @Test
    void selfRemovalAndConcurrentAdministratorRemovalLeaveOneUsableAdmin() throws Exception {
        long first = account("first@example.test", "ADMIN"), second = account("second@example.test", "ADMIN");
        String a = login("first@example.test"), b = login("second@example.test");
        expect(call("POST", "/admin/users/" + first + "/deactivate", a, null), 409);
        expect(call("DELETE", "/admin/users/" + first, a, null), 409);
        statuses(race(() -> call("POST", "/admin/users/" + second + "/deactivate", a, null),
                () -> call("POST", "/admin/users/" + first + "/deactivate", b, null)), 200, 401);
        assertEquals(1, count("select count(*) from users where role='ADMIN' and active"));
    }

    @Test
    void deletionAndSendingCannotLeaveDanglingRequestsOrLeakedEvents() throws Exception {
        account("admin@example.test", "ADMIN");
        long target = student("target@example.test", "Target", "target-private");
        student("sender@example.test", "Sender", "sender-private");
        String admin = login("admin@example.test"), sender = login("sender@example.test");
        var replies = race(() -> call("DELETE", "/admin/users/" + target, admin, null),
                () -> call("POST", "/match-requests", sender, Map.of("receiverId", target)));
        assertEquals(204, replies.get(0).status());
        assertTrue(List.of(201, 404).contains(replies.get(1).status()), replies.get(1).body());
        assertEquals(0, count("select count(*) from match_requests"));
        assertEquals(0, count("select count(*) from notifications where resource_type='MATCH_REQUEST'"));
        assertEquals(0, count("select count(*) from users where id=?", target));
    }

    @Test
    void deactivationAndGroupApprovalCannotRestoreMembership() throws Exception {
        account("admin@example.test", "ADMIN");
        long target = student("target@example.test", "Target", "target-private");
        student("leader@example.test", "Leader", "leader-private");
        String admin = login("admin@example.test"), t = login("target@example.test"), l = login("leader@example.test");
        long group = group(l, 2), request = apply(group, t);
        var replies = race(() -> call("POST", "/admin/users/" + target + "/deactivate", admin, null),
                () -> approve(group, request, l));
        assertEquals(200, replies.get(0).status());
        assertTrue(List.of(200, 404, 409).contains(replies.get(1).status()), replies.get(1).body());
        assertEquals(0, count("select count(*) from group_memberships where student_id=?", target));
        assertEquals(0, count("select count(*) from group_join_requests where student_id=? and status='PENDING'", target));
    }

    @Test
    void invalidAdminCreateDoesNotLeaveAnOrphanAccount() {
        account("admin@example.test", "ADMIN");
        String admin = login("admin@example.test");
        Map<String, Object> invalid = studentDetails("invalid@example.test", "Invalid");
        invalid.put("yearOfStudy", 0);
        expect(call("POST", "/admin/users", admin, invalid), 400);
        invalid.put("yearOfStudy", 3);
        invalid.put("password", "🔑".repeat(30));
        expect(call("POST", "/admin/users", admin, invalid), 400);
        assertEquals(1, count("select count(*) from users"));
        assertEquals(0, count("select count(*) from students"));
    }

    private Map<String, Object> studentDetails(String email, String name) {
        return new HashMap<>(Map.of("email", email, "password", "test-password-only", "role", "STUDENT",
                "name", name, "school", "SCIS", "programme", "Information Systems", "yearOfStudy", 3,
                "contactNumber", "private-created-contact"));
    }

    private void assertPrivateAdminPayload(String payload) {
        assertFalse(payload.contains("contactNumber"));
        assertFalse(payload.contains("private-created-contact"));
        assertFalse(payload.contains("replacement-private-contact"));
        assertFalse(payload.contains("password"));
    }
}
