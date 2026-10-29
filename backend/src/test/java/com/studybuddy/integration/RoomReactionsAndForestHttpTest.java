package com.studybuddy.integration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

/** Real PostgreSQL, JWT and HTTP coverage of room reactions and the one-tree-per-visit forest. */
class RoomReactionsAndForestHttpTest extends PostgresHttpTest {
    private long memberId;
    private long groupId;
    private String leader;
    private String member;
    private String outsider;
    @Autowired private ThreadPoolTaskExecutor forestUpdates;

    @BeforeEach
    void members() {
        student("leader@example.test", "Leader", "81110000");
        memberId = student("member@example.test", "Member", "82220000");
        student("outsider@example.test", "Outsider", "84440000");
        leader = login("leader@example.test");
        member = login("member@example.test");
        outsider = login("outsider@example.test");
        groupId = group(leader, 4);
        expect(approve(groupId, apply(groupId, member), leader), 200);
    }

    @Test
    void onlyStudentsInTheRoomCanReactAndEveryoneThereSeesIt() {
        expect(call("POST", reactions(), null, Map.of("type", "CLAP")), 401);
        expect(call("POST", reactions(), outsider, Map.of("type", "CLAP")), 403);
        expect(call("POST", reactions(), member, Map.of("type", "CLAP")), 409);
        join(member, UUID.randomUUID());
        expect(call("POST", reactions(), member, Map.of("type", "WAVE")), 400);
        expect(call("POST", reactions(), member, Map.of()), 400);
        JsonNode sent = expect(call("POST", reactions(), member, Map.of("type", "HEART")), 201);
        assertEquals("HEART", sent.path("type").asString());
        assertEquals("Member", sent.path("senderName").asString());

        JsonNode seen = state(leader).path("reactions");
        assertEquals(1, seen.size());
        assertEquals(sent.path("id").asLong(), seen.get(0).path("id").asLong(), "The sender's copy has the id everyone else sees");
        assertEquals("HEART", seen.get(0).path("type").asString());
        assertEquals("Member", seen.get(0).path("senderName").asString());
        assertEquals(memberId, seen.get(0).path("studentId").asLong());
        var reply = call("GET", path(), leader, null);
        assertFalse(reply.body().contains("82220000"), "Reactions never carry contact details");
    }

    @Test
    void reactionsAreRateLimitedAndOldOnesDropOut() {
        join(member, UUID.randomUUID());
        for (int i = 0; i < 5; i++) expect(call("POST", reactions(), member, Map.of("type", "FIRE")), 201);
        expect(call("POST", reactions(), member, Map.of("type", "FIRE")), 409);
        database.update("update room_reactions set sent_at=now()-interval '1 minute'");
        assertEquals(0, state(member).path("reactions").size());
        expect(call("POST", reactions(), member, Map.of("type", "SPROUT")), 201);
        assertEquals(1, count("select count(*) from room_reactions"), "Old reactions are pruned when someone reacts");
    }

    @Test
    void focusTimeInAVisitGrowsOneTreeThatIsPlantedOnLeaving() {
        UUID tab = UUID.randomUUID();
        join(member, tab);
        var growing = expect(call("GET", "/forest/me/rooms/" + groupId, member, null), 200);
        assertTrue(growing.path("visiting").asBoolean());
        assertEquals("SPROUT", growing.path("next").asString());
        assertEquals(1, count("select count(*) from focus_visits where student_id=?", memberId));

        startTimer();
        database.update("update study_rooms set timer_anchor=timer_anchor-interval '60 seconds' where study_group_id=?", groupId);
        database.update("update focus_visits set last_seen_at=last_seen_at-interval '60 seconds', started_at=started_at-interval '60 seconds'");
        heartbeat(member, tab);
        long credited = count("select focused_millis from focus_visits where student_id=?", memberId);
        assertTrue(credited >= 59_000 && credited <= 62_000, "Credited " + credited);

        database.update("update focus_visits set focused_millis=30*60000");
        assertEquals("SAPLING", expect(call("GET", "/forest/me/rooms/" + groupId, member, null), 200).path("species").asString());
        assertEquals(0, forest(member).path("treeCount").asInt(), "A growing tree is not planted yet");

        leave(member, tab);
        var forest = forest(member);
        assertEquals(1, forest.path("treeCount").asInt());
        assertEquals("SAPLING", forest.path("trees").get(0).path("species").asString());
        assertEquals(30, forest.path("trees").get(0).path("focusedMinutes").asLong());
        assertFalse(forest.path("trees").get(0).path("light").isMissingNode());
        int variant = forest.path("trees").get(0).path("variant").asInt(-1);
        assertTrue(variant >= 0 && variant < forest.path("variantsPerSpecies").asInt(), "Variant " + variant);
        assertEquals(count("select plot from focus_visits where student_id=?", memberId), forest.path("trees").get(0).path("plot").asLong(),
            "The plot drawn when the visit opened is kept");
        assertEquals(5, forest.path("stages").size());
        assertEquals(0, forest(leader).path("treeCount").asInt(), "Each student sees only their own forest");
        JsonNode forester = null;
        for (JsonNode badge : expect(call("GET", "/achievements/me", member, null), 200).path("badges")) {
            if ("FORESTER".equals(badge.path("key").asString())) forester = badge;
        }
        assertNotNull(forester);
        assertEquals(1, forester.path("current").asInt());
        assertEquals("BRONZE", forester.path("tier").asString(), "The first planted tree earns Forester bronze");
        assertFalse(expect(call("GET", "/forest/me/rooms/" + groupId, member, null), 200).path("visiting").asBoolean());
    }

    @Test
    void shortVisitsPlantNothingAndAGapStartsANewTree() {
        UUID tab = UUID.randomUUID();
        join(member, tab);
        database.update("update focus_visits set focused_millis=5*60000, last_seen_at=now()-interval '5 minutes', started_at=now()-interval '20 minutes'");
        heartbeat(member, tab);
        assertEquals(2, count("select count(*) from focus_visits where student_id=?", memberId));
        assertEquals(1, count("select count(*) from focus_visits where student_id=? and ended_at is null", memberId));
        assertEquals(0, forest(member).path("treeCount").asInt(), "Five focused minutes is too short for a tree");
    }

    @Test
    void twoTabsShareOneVisitAndClosingOneKeepsItGrowing() {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        join(member, first);
        join(member, second);
        assertEquals(1, count("select count(*) from focus_visits where student_id=?", memberId));
        leave(member, first);
        assertEquals(1, count("select count(*) from focus_visits where student_id=? and ended_at is null", memberId));
        leave(member, second);
        assertEquals(0, count("select count(*) from focus_visits where student_id=? and ended_at is null", memberId));
    }

    @Test
    void forestIsOnlyForTheSignedInStudent() {
        expect(call("GET", "/forest/me", null, null), 401);
        expect(call("GET", "/forest/me/rooms/" + groupId, null, null), 401);
        account("admin@example.test", "ADMIN");
        String admin = login("admin@example.test");
        expect(call("GET", "/forest/me", admin, null), 403);
        expect(call("GET", "/forest/me/rooms/" + groupId, admin, null), 403);
        assertEquals(0, forest(member).path("treeCount").asInt());
    }

    @Test
    void twoTabsRenewingAtOnceCreditTheSameStretchOnlyOnce() throws Exception {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        join(member, first);
        join(member, second);
        startTimer();
        database.update("update study_rooms set timer_anchor=timer_anchor-interval '60 seconds' where study_group_id=?", groupId);
        database.update("update focus_visits set last_seen_at=last_seen_at-interval '60 seconds', started_at=started_at-interval '60 seconds'");
        statuses(race(() -> renew(member, first), () -> renew(member, second)), 200, 200);
        awaitForest();
        long credited = count("select focused_millis from focus_visits where student_id=?", memberId);
        assertTrue(credited >= 59_000 && credited <= 63_000, "One minute credited once, not twice: " + credited);
        assertEquals(1, count("select count(*) from focus_visits where student_id=?", memberId));
    }

    @Test
    void aTickFromBeforeLeavingDoesNotReopenTheVisit() {
        UUID tab = UUID.randomUUID();
        join(member, tab);
        leave(member, tab);
        database.update("update focus_visits set ended_at=now()+interval '1 minute'");
        join(member, tab);
        assertEquals(0, count("select count(*) from focus_visits where student_id=? and ended_at is null", memberId),
            "A tick older than the visit's end is ignored");
    }

    private void startTimer() {
        long version = state(leader).path("version").asLong();
        expect(call("POST", path() + "/timer", leader, Map.of("command", "START", "expectedVersion", version)), 200);
    }
    private String path() { return "/groups/" + groupId + "/room"; }
    private String reactions() { return path() + "/reactions"; }
    private JsonNode state(String token) { return expect(call("GET", path(), token, null), 200); }
    private JsonNode forest(String token) { return expect(call("GET", "/forest/me", token, null), 200); }
    /** Room calls hand forest updates to a worker; these helpers wait for it so checks see the result. */
    private JsonNode join(String token, UUID tab) {
        var room = expect(call("POST", path() + "/join", token, Map.of("clientId", tab)), 200);
        awaitForest();
        return room;
    }
    private JsonNode heartbeat(String token, UUID tab) {
        var room = expect(renew(token, tab), 200);
        awaitForest();
        return room;
    }
    private Reply renew(String token, UUID tab) {
        return call("PUT", path() + "/presence", token, Map.of("clientId", tab, "presence", "PRESENT"));
    }
    private void leave(String token, UUID tab) {
        expect(call("DELETE", path() + "/presence/" + tab, token, null), 204);
        awaitForest();
    }
    private void awaitForest() {
        var pool = forestUpdates.getThreadPoolExecutor();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (pool.getCompletedTaskCount() < pool.getTaskCount()) {
            if (System.nanoTime() > deadline) fail("Forest updates did not finish");
            Thread.onSpinWait();
        }
    }
}
