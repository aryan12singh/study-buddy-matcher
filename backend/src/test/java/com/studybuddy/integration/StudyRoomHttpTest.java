package com.studybuddy.integration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

/** Real PostgreSQL, JWT and HTTP coverage of the private room foundation. */
class StudyRoomHttpTest extends PostgresHttpTest {
    private long leaderId;
    private long memberId;
    private long otherId;
    private long groupId;
    private String leader;
    private String member;
    private String other;
    private String outsider;
    private String admin;

    @BeforeEach
    void members() {
        leaderId = student("leader@example.test", "Leader", "81110000");
        memberId = student("member@example.test", "Member", "82220000");
        otherId = student("other@example.test", "Other", "83330000");
        student("outsider@example.test", "Outsider", "84440000");
        account("admin@example.test", "ADMIN");
        leader = login("leader@example.test");
        member = login("member@example.test");
        other = login("other@example.test");
        outsider = login("outsider@example.test");
        admin = login("admin@example.test");
        groupId = group(leader, 4);
        expect(approve(groupId, apply(groupId, member), leader), 200);
        expect(approve(groupId, apply(groupId, other), leader), 200);
    }

    @Test
    void onlyAcceptedActiveStudentsCanReadOrJoinAndPayloadsWithholdContacts() {
        expect(call("GET", path(), null, null), 401);
        expect(call("GET", path(), admin, null), 403);
        expect(call("GET", path(), outsider, null), 403);
        apply(groupId, outsider);
        expect(call("POST", path() + "/join", outsider, Map.of("clientId", UUID.randomUUID())), 403);
        var reply = call("GET", path(), member, null);
        var room = expect(reply, 200);
        assertEquals(0, room.path("participants").size());
        assertFalse(room.path("canControl").asBoolean());
        assertEquals(leaderId, room.path("hostId").asLong());
        for (String privateValue : new String[]{"contactNumber", "passwordHash", "email", "82220000"}) {
            assertFalse(reply.body().contains(privateValue), reply.body());
        }
        assertEquals(0, count("select count(*) from study_rooms"), "Reading defaults does not create a room");
    }

    @Test
    void joiningIsIdempotentPerTabAndCapacityCountsStudents() {
        configure(0, 25, 1, null, null);
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        join(member, first);
        join(member, first);
        var room = join(member, second);
        assertEquals(1, room.path("participants").size());
        assertEquals(2, count("select count(*) from room_presences"));
        expect(call("POST", path() + "/join", other, Map.of("clientId", UUID.randomUUID())), 409);
        expect(call("DELETE", path() + "/presence/" + first, other, null), 204);
        assertEquals(2, count("select count(*) from room_presences"), "Cannot delete another student's lease");
        expect(call("DELETE", path() + "/presence/" + first, member, null), 204);
        assertEquals(1, state(member).path("participants").size());
        expect(call("DELETE", path() + "/presence/" + second, member, null), 204);
        assertEquals(0, state(member).path("participants").size());
        join(other, UUID.randomUUID());
    }

    @Test
    void concurrentJoinsCannotOverfillTheLastRoomPlace() throws Exception {
        configure(0, 25, 1, null, null);
        statuses(race(() -> call("POST", path() + "/join", member, Map.of("clientId", UUID.randomUUID())),
            () -> call("POST", path() + "/join", other, Map.of("clientId", UUID.randomUUID()))), 200, 409);
        assertEquals(1, state(leader).path("participants").size());
    }

    @Test
    void expiredPresenceFreesCapacityAndCannotBeRevivedByHeartbeat() {
        configure(0, 25, 1, null, null);
        UUID tab = UUID.randomUUID();
        join(member, tab);
        expire(memberId);
        expect(call("PUT", path() + "/presence", member, Map.of("clientId", tab, "presence", "FOCUS")), 409);
        assertEquals(0, state(leader).path("participants").size());
        join(other, UUID.randomUUID());
        expect(call("POST", path() + "/join", member, Map.of("clientId", tab)), 409);
        assertEquals(1, count("select count(*) from room_presences"), "Expired leases are pruned on join");
    }

    @Test
    void hostsMustBePresentAndOnlyLeaderCanAssignAcceptedMembers() {
        var settings = configure(0, 25, 4, memberId, otherId);
        long version = settings.path("version").asLong();
        expect(command(member, "START", version), 403);
        UUID tab = UUID.randomUUID();
        join(member, tab);
        assertTrue(state(member).path("canControl").asBoolean());
        var started = expect(command(member, "START", version), 200);
        expect(call("PUT", path() + "/settings", member, configuration(started.path("version").asLong(), 25, 4, memberId, otherId)), 403);
        join(other, UUID.randomUUID());
        expect(command(other, "PAUSE", started.path("version").asLong()), 200);
        expect(call("PUT", path() + "/presence", member, Map.of("clientId", tab, "presence", "FOCUS")), 200);
        assertEquals("FOCUS", state(member).path("participants").get(0).path("presence").asString());
        expire(memberId);
        assertFalse(state(member).path("canControl").asBoolean());
        assertFalse(state(leader).path("hostOnline").asBoolean());
        assertTrue(state(leader).path("canControl").asBoolean(), "Leader can recover controls without an online host");
        long outsiderId = count("select id from users where email='outsider@example.test'");
        expect(call("PUT", path() + "/settings", leader, configuration(state(leader).path("version").asLong(), 25, 4, outsiderId, null)), 400);
    }

    @Test
    void timerSurvivesDisconnectsAndPauseResumeResetUseCurrentVersion() {
        configure(0, 1, 4, null, null);
        var started = expect(command(leader, "START", state(leader).path("version").asLong()), 200);
        expect(command(leader, "START", started.path("version").asLong()), 409);
        expect(command(leader, "PAUSE", 0), 409);
        database.update("update study_rooms set timer_anchor=now()-interval '70 seconds' where study_group_id=?", groupId);
        var reconnect = state(member);
        assertEquals("BREAK", reconnect.path("timer").path("phase").asString());
        assertEquals("RUNNING", reconnect.path("timer").path("status").asString());
        var paused = expect(command(leader, "PAUSE", reconnect.path("version").asLong()), 200);
        assertEquals(paused.path("timer"), state(other).path("timer"));
        var resumed = expect(command(leader, "RESUME", paused.path("version").asLong()), 200);
        assertEquals("RUNNING", resumed.path("timer").path("status").asString());
        var reset = expect(command(leader, "RESET", resumed.path("version").asLong()), 200);
        assertEquals("IDLE", reset.path("timer").path("status").asString());
        assertEquals("FOCUS", reset.path("timer").path("phase").asString());
        assertEquals(60_000, reset.path("timer").path("remainingMillis").asLong());
        expect(command(leader, "RESUME", reset.path("version").asLong()), 409);
    }

    @Test
    void settingsValidateBoundsRolesCapacityAndDurationChanges() {
        long version = state(leader).path("version").asLong();
        for (int invalidFocus : new int[]{0, 181}) {
            expect(call("PUT", path() + "/settings", leader, configuration(version, invalidFocus, 4, null, null)), 400);
        }
        expect(call("PUT", path() + "/settings", leader, configuration(version, 25, 5, null, null)), 400);
        expect(call("PUT", path() + "/settings", leader, configuration(version, 25, 4, memberId, memberId)), 400);
        join(member, UUID.randomUUID()); join(other, UUID.randomUUID());
        expect(call("PUT", path() + "/settings", leader, configuration(version, 25, 1, null, null)), 400);
        var started = expect(command(leader, "START", version), 200);
        expect(call("PUT", path() + "/settings", leader, configuration(started.path("version").asLong(), 30, 4, null, null)), 409);
        var reset = expect(command(leader, "RESET", started.path("version").asLong()), 200);
        var changed = configure(reset.path("version").asLong(), 30, 4, memberId, null);
        assertEquals(1_800_000, changed.path("timer").path("remainingMillis").asLong());
    }

    @Test
    void concurrentControlsRejectAStaleCommandInsteadOfOverwriting() throws Exception {
        join(leader, UUID.randomUUID());
        statuses(race(() -> command(leader, "START", 0),
            () -> call("PUT", path() + "/audio", leader, Map.of("preset", "RAIN", "playing", true, "expectedVersion", 0))), 200, 409);
        assertEquals(1, state(leader).path("version").asLong());
    }

    @Test
    void sharedAudioValidatesRolesAndNeverStoresLocalVolume() {
        join(member, UUID.randomUUID());
        expect(call("PUT", path() + "/audio", member, Map.of("preset", "RAIN", "playing", true, "expectedVersion", 0)), 403);
        var room = expect(call("PUT", path() + "/audio", leader, Map.of("preset", "RAIN", "playing", true, "expectedVersion", 0)), 200);
        assertEquals("RAIN", room.path("audio").path("preset").asString());
        assertTrue(state(member).path("audio").path("playing").asBoolean());
        assertEquals(2, room.path("audio").size());
        expect(call("PUT", path() + "/audio", leader, Map.of("preset", "UNKNOWN", "playing", true, "expectedVersion", 1)), 400);
    }

    @Test
    void removedAndDeactivatedMembersLosePresenceAndControlsImmediately() {
        configure(0, 25, 4, memberId, otherId);
        join(member, UUID.randomUUID()); join(other, UUID.randomUUID());
        expect(call("DELETE", "/groups/" + groupId + "/members/" + memberId, leader, null), 204);
        expect(call("GET", path(), member, null), 403);
        var remaining = state(leader);
        assertEquals(leaderId, remaining.path("hostId").asLong());
        assertEquals(1, remaining.path("participants").size());
        expect(call("POST", "/admin/users/" + otherId + "/deactivate", admin, null), 200);
        expect(call("GET", path(), other, null), 401);
        assertEquals(0, state(leader).path("participants").size());
        assertTrue(state(leader).path("coHostId").isNull());
        expect(call("POST", "/groups/" + groupId + "/close", leader, null), 200);
        expect(call("GET", path(), leader, null), 409);
        expect(command(leader, "START", 0), 409);
    }

    @Test
    void accountDeletionCascadesLeasesAndLeaderDeletionRemovesTheRoom() {
        configure(0, 25, 4, memberId, null);
        join(member, UUID.randomUUID());
        expect(call("DELETE", "/admin/users/" + memberId, admin, null), 204);
        assertEquals(0, count("select count(*) from room_presences"));
        assertEquals(leaderId, state(leader).path("hostId").asLong());
        expect(call("DELETE", "/admin/users/" + leaderId, admin, null), 204);
        assertEquals(0, count("select count(*) from study_rooms"));
        expect(call("GET", path(), other, null), 404);
    }

    @Test
    void invalidInputDoesNotCreatePresenceOrChangeRoomState() {
        expect(call("POST", path() + "/join", member, Map.of("clientId", "invalid")), 400);
        expect(call("POST", path() + "/join", member, Map.of()), 400);
        expect(call("POST", path() + "/timer", leader, Map.of("command", "START", "expectedVersion", -1)), 400);
        expect(call("PUT", path() + "/presence", member, Map.of("clientId", UUID.randomUUID(), "presence", "UNKNOWN")), 400);
        assertEquals(0, count("select count(*) from study_rooms"));
        assertEquals(0, count("select count(*) from room_presences"));
    }

    private String path() { return "/groups/" + groupId + "/room"; }
    private JsonNode state(String token) { return expect(call("GET", path(), token, null), 200); }
    private JsonNode join(String token, UUID tab) {
        return expect(call("POST", path() + "/join", token, Map.of("clientId", tab)), 200);
    }
    private Reply command(String token, String command, long version) {
        return call("POST", path() + "/timer", token, Map.of("command", command, "expectedVersion", version));
    }
    private JsonNode configure(long version, int focus, int capacity, Long host, Long coHost) {
        return expect(call("PUT", path() + "/settings", leader, configuration(version, focus, capacity, host, coHost)), 200);
    }
    private Map<String, Object> configuration(long version, int focus, int capacity, Long host, Long coHost) {
        Map<String, Object> input = new HashMap<>();
        input.put("focusMinutes", focus); input.put("breakMinutes", 1); input.put("participantLimit", capacity);
        input.put("hostId", host); input.put("coHostId", coHost); input.put("expectedVersion", version);
        return input;
    }
    private void expire(long studentId) {
        database.update("update room_presences set observed_at=now()-interval '2 minutes', expires_at=now()-interval '1 minute' where student_id=?", studentId);
    }
}
