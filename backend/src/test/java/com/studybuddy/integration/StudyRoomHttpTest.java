package com.studybuddy.integration;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

/** Real PostgreSQL, JWT and HTTP coverage of private room controls and access lifecycle. */
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
    void pollingDoesNotWaitForAHeldGroupLock() throws Exception {
        UUID tab = UUID.randomUUID();
        join(member, tab);
        try (Connection holder = database.getDataSource().getConnection()) {
            holder.setAutoCommit(false);
            try (var lock = holder.prepareStatement("select id from study_groups where id = ? for update")) {
                lock.setLong(1, groupId);
                lock.executeQuery();
            }
            try {
                var read = CompletableFuture.supplyAsync(() -> call("GET", path(), other, null));
                var heartbeat = CompletableFuture.supplyAsync(() ->
                    call("PUT", path() + "/presence", member, Map.of("clientId", tab, "presence", "FOCUS")));
                expect(read.get(5, TimeUnit.SECONDS), 200);
                expect(heartbeat.get(5, TimeUnit.SECONDS), 200);
            } finally {
                holder.rollback();
            }
        }
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
        expect(call("PUT", path() + "/settings", member, configuration(started.path("version").asLong(), 25, 4, leaderId, otherId)), 403);
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
    void startingTheTimerInvitesAbsentMembersOnceUntilTheyReadTheInvitation() {
        String invitations = "select count(*) from notifications where type='GROUP_STUDY_ROOM_STARTED' and recipient_id=?";
        join(member, UUID.randomUUID());
        var started = expect(command(leader, "START", state(leader).path("version").asLong()), 200);
        assertEquals(1, count(invitations, otherId));
        assertEquals(0, count(invitations, memberId));
        assertEquals(0, count(invitations, leaderId));
        assertEquals(1, count("select count(*) from notifications where type='GROUP_STUDY_ROOM_STARTED' and resource_type='GROUP' and resource_id=?", groupId));

        var paused = expect(command(leader, "PAUSE", started.path("version").asLong()), 200);
        var resumed = expect(command(leader, "RESUME", paused.path("version").asLong()), 200);
        var reset = expect(command(leader, "RESET", resumed.path("version").asLong()), 200);
        var restarted = expect(command(leader, "START", reset.path("version").asLong()), 200);
        assertEquals(1, count(invitations, otherId));

        database.update("update notifications set read=true where type='GROUP_STUDY_ROOM_STARTED' and recipient_id=?", otherId);
        var resetAgain = expect(command(leader, "RESET", restarted.path("version").asLong()), 200);
        expect(command(leader, "START", resetAgain.path("version").asLong()), 200);
        assertEquals(2, count(invitations, otherId));
    }

    @Test
    void aDatabaseThatRejectsTheInvitationStillStartsTheTimer() {
        // Reproduces a database missing the GROUP_STUDY_ROOM_STARTED migration.
        String allowed = database.queryForObject(
            "select pg_get_constraintdef(oid) from pg_constraint where conname='notifications_type_check'", String.class);
        database.execute("alter table notifications drop constraint notifications_type_check");
        database.execute("alter table notifications add constraint notifications_type_check check (type <> 'GROUP_STUDY_ROOM_STARTED')");
        try {
            var started = expect(command(leader, "START", state(leader).path("version").asLong()), 200);
            assertEquals("RUNNING", started.path("timer").path("status").asString());
            assertEquals("RUNNING", state(member).path("timer").path("status").asString());
            assertEquals(0, count("select count(*) from notifications where type='GROUP_STUDY_ROOM_STARTED'"));
        } finally {
            database.execute("alter table notifications drop constraint notifications_type_check");
            database.execute("alter table notifications add constraint notifications_type_check " + allowed);
        }
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
    void presentHostsCanChangeDurationsAndCapacityWithoutAssigningRoles() {
        var room = configure(0, 25, 4, memberId, otherId);
        long version = room.path("version").asLong();
        expect(call("PUT", path() + "/settings", member, configuration(version, 30, 2, memberId, otherId)), 403);
        UUID hostTab = UUID.randomUUID();
        join(member, hostTab);
        var changed = expect(call("PUT", path() + "/settings", member, configuration(version, 30, 2, memberId, otherId)), 200);
        assertEquals(30, changed.path("focusMinutes").asInt());
        assertEquals(2, changed.path("participantLimit").asInt());
        version = changed.path("version").asLong();
        expect(call("PUT", path() + "/settings", member, configuration(version, 30, 2, otherId, memberId)), 403);
        expect(call("PUT", path() + "/settings", member, configuration(version - 1, 30, 2, memberId, otherId)), 409);
        join(other, UUID.randomUUID());
        var coHostSaved = expect(call("PUT", path() + "/settings", other, configuration(version, 30, 3, memberId, otherId)), 200);
        assertEquals(3, coHostSaved.path("participantLimit").asInt());
        expect(call("DELETE", path() + "/presence/" + hostTab, member, null), 204);
        expect(call("PUT", path() + "/settings", member,
            configuration(coHostSaved.path("version").asLong(), 35, 3, memberId, otherId)), 403);
    }

    @Test
    void concurrentControlsRejectAStaleCommandInsteadOfOverwriting() throws Exception {
        join(leader, UUID.randomUUID());
        statuses(race(() -> command(leader, "START", 0),
            () -> call("PUT", path() + "/audio", leader, audio(0, null, false, "RAIN", true))), 200, 409);
        assertEquals(1, state(leader).path("version").asLong());
    }

    @Test
    void sharedAudioValidatesRolesAndNeverStoresLocalVolume() {
        join(member, UUID.randomUUID());
        expect(call("PUT", path() + "/audio", member, audio(0, "CALM_MUSIC", true, "RAIN", true)), 403);
        var room = expect(call("PUT", path() + "/audio", leader, audio(0, "CALM_MUSIC", true, "RAIN", true)), 200);
        assertEquals("CALM_MUSIC", room.path("audio").path("musicPreset").asString());
        assertEquals("RAIN", room.path("audio").path("ambientPreset").asString());
        assertTrue(state(member).path("audio").path("musicPlaying").asBoolean());
        assertTrue(state(member).path("audio").path("ambientPlaying").asBoolean());
        assertEquals(4, room.path("audio").size(), "Only shared selections/playback are persisted; volumes stay local");
        long version = room.path("version").asLong();
        expect(call("PUT", path() + "/audio", leader, audio(version, "UNKNOWN", true, null, false)), 400);
        expect(call("PUT", path() + "/audio", leader, audio(version, "RAIN", true, null, false)), 400);
        expect(call("PUT", path() + "/audio", leader, audio(version, null, false, "CALM_MUSIC", true)), 400);
        expect(call("PUT", path() + "/audio", leader, audio(version, null, true, "RAIN", true)), 400);
        expect(call("PUT", path() + "/audio", leader, audio(version, "CALM_MUSIC", true, null, true)), 400);
        assertEquals(room.path("audio"), state(member).path("audio"), "Rejected selections never partially update a layer");
        var musicOnly = expect(call("PUT", path() + "/audio", leader, audio(version, "CALM_MUSIC", true, null, false)), 200);
        assertTrue(musicOnly.path("audio").path("musicPlaying").asBoolean());
        assertTrue(musicOnly.path("audio").path("ambientPreset").isNull());
        var silent = expect(call("PUT", path() + "/audio", leader,
            audio(musicOnly.path("version").asLong(), null, false, null, false)), 200);
        assertFalse(silent.path("audio").path("musicPlaying").asBoolean());
        assertFalse(silent.path("audio").path("ambientPlaying").asBoolean());
    }

    @Test
    void removedAndDeactivatedMembersLosePresenceAndControlsImmediately() {
        configure(0, 25, 4, memberId, otherId);
        join(member, UUID.randomUUID()); join(other, UUID.randomUUID());
        expect(call("DELETE", "/groups/" + groupId + "/members/" + memberId, leader, null), 204);
        assertEquals(0, count("select count(*) from room_presences where student_id=?", memberId));
        expect(call("GET", path(), member, null), 403);
        var remaining = state(leader);
        assertEquals(leaderId, remaining.path("hostId").asLong());
        assertEquals(1, remaining.path("participants").size());
        expect(call("POST", "/admin/users/" + otherId + "/deactivate", admin, null), 200);
        expect(call("GET", path(), other, null), 401);
        assertEquals(0, state(leader).path("participants").size());
        assertTrue(state(leader).path("coHostId").isNull());
        join(leader, UUID.randomUUID());
        var started = expect(command(leader, "START", state(leader).path("version").asLong()), 200);
        expect(call("PUT", path() + "/audio", leader,
            audio(started.path("version").asLong(), "CALM_MUSIC", true, "RAIN", true)), 200);
        expect(call("POST", "/groups/" + groupId + "/close", leader, null), 200);
        assertEquals(0, count("select count(*) from room_presences"));
        assertEquals("IDLE", database.queryForObject("select timer_status from study_rooms where study_group_id=?", String.class, groupId));
        assertEquals(0, count("select count(*) from study_rooms where music_playing or ambient_playing"));
        expect(call("GET", path(), leader, null), 409);
        expect(command(leader, "START", 0), 409);
    }

    @Test
    void removalAndReadmissionNeverReviveOldLeasesOrRolesOrBypassCapacity() {
        configure(0, 25, 1, memberId, null);
        UUID oldTab = UUID.randomUUID();
        join(member, oldTab);
        join(member, UUID.randomUUID());
        assertEquals(2, count("select count(*) from room_presences where student_id=?", memberId));
        expect(call("DELETE", "/groups/" + groupId + "/members/" + memberId, leader, null), 204);
        assertEquals(0, count("select count(*) from room_presences where student_id=?", memberId));
        join(other, UUID.randomUUID());
        expect(approve(groupId, apply(groupId, member), leader), 200);
        expect(call("PUT", path() + "/presence", member, Map.of("clientId", oldTab, "presence", "FOCUS")), 409);
        expect(call("POST", path() + "/join", member, Map.of("clientId", oldTab)), 409);
        var room = state(member);
        assertEquals(1, room.path("participants").size());
        assertEquals(leaderId, room.path("hostId").asLong());
        assertFalse(room.path("canControl").asBoolean());
        assertEquals(1, count("select count(*) from room_presences"));
    }

    @Test
    void deactivationAndReadmissionRequireFreshPresenceAndExplicitRoleAssignment() {
        configure(0, 25, 1, memberId, null);
        UUID oldTab = UUID.randomUUID();
        join(member, oldTab);
        expect(call("POST", "/admin/users/" + memberId + "/deactivate", admin, null), 200);
        assertEquals(0, count("select count(*) from room_presences where student_id=?", memberId));
        join(other, UUID.randomUUID());
        expect(call("POST", "/admin/users/" + memberId + "/reactivate", admin, null), 200);
        String readmitted = login("member@example.test");
        expect(approve(groupId, apply(groupId, readmitted), leader), 200);
        expect(call("PUT", path() + "/presence", readmitted, Map.of("clientId", oldTab, "presence", "PRESENT")), 409);
        expect(call("POST", path() + "/join", readmitted, Map.of("clientId", oldTab)), 409);
        assertFalse(state(readmitted).path("canControl").asBoolean());
        assertEquals(leaderId, state(readmitted).path("hostId").asLong());
        assertEquals(1, state(leader).path("participants").size());
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
    private Map<String, Object> audio(long version, String music, boolean playMusic, String ambient, boolean playAmbient) {
        Map<String, Object> input = new HashMap<>();
        input.put("musicPreset", music); input.put("musicPlaying", playMusic);
        input.put("ambientPreset", ambient); input.put("ambientPlaying", playAmbient);
        input.put("expectedVersion", version);
        return input;
    }
    private void expire(long studentId) {
        database.update("update room_presences set observed_at=now()-interval '2 minutes', expires_at=now()-interval '1 minute' where student_id=?", studentId);
    }
}
