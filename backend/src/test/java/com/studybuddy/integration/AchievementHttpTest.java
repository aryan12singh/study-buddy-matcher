package com.studybuddy.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Real HTTP, authorization and read-query coverage against the isolated migrated PostgreSQL database. */
class AchievementHttpTest extends PostgresHttpTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Singapore");

    @Test
    void onlyStudentsCanReadTheirOwnStandingAndProfilesNeverCarryIt() {
        long owner = student("owner@example.test", "Owner", "91234561");
        long other = student("other@example.test", "Other", "91234562");
        account("admin@example.test", "ADMIN");
        String token = login("owner@example.test");
        send(other, token);

        JsonNode mine = expect(call("GET", "/achievements/me", token, null), 200);
        assertEquals(1, badge(mine, "FIRST_MOVE").path("current").asInt());
        assertEquals(0, badge(standing(login("other@example.test")), "FIRST_MOVE").path("current").asInt());
        assertEquals("Asia/Singapore", mine.path("streak").path("weekZone").asString());
        expect(call("GET", "/achievements/me", null, null), 401);
        expect(call("GET", "/achievements/me", login("admin@example.test"), null), 403);
        // An arbitrary selector cannot turn /me into another student's data.
        JsonNode ignoredSelector = expect(call("GET", "/achievements/me?studentId=" + other, token, null), 200);
        assertEquals(1, badge(ignoredSelector, "FIRST_MOVE").path("current").asInt());
        JsonNode publicProfile = expect(call("GET", "/students/" + owner + "/profile", login("other@example.test"), null), 200);
        assertFalse(publicProfile.has("achievements"));
        assertFalse(publicProfile.has("badges"));
    }

    @Test
    void earnedTiersStayWhenConnectionsAndOpenMembershipsEnd() {
        var people = people();
        long group = group(people.leaderToken(), 3);
        expect(approve(group, apply(group, people.memberToken()), people.leaderToken()), 200);
        long connection = connect(people);
        assertEquals(1, badge(standing(people.memberToken()), "TEAM_PLAYER").path("current").asInt());
        assertEquals(0, badge(standing(people.leaderToken()), "TEAM_PLAYER").path("current").asInt());
        assertEquals("BRONZE", badge(standing(people.leaderToken()), "CONNECTOR").path("tier").asString());

        expect(call("DELETE", "/connections/" + connection, people.memberToken(), null), 204);
        expect(call("POST", "/groups/" + group + "/close", people.leaderToken(), null), 200);

        JsonNode member = standing(people.memberToken());
        JsonNode leader = standing(people.leaderToken());
        assertEquals(0, badge(member, "TEAM_PLAYER").path("current").asInt());
        assertEquals("BRONZE", badge(member, "TEAM_PLAYER").path("tier").asString());
        assertTrue(badge(member, "TEAM_PLAYER").path("currentTier").isNull());
        assertEquals(0, badge(leader, "CONNECTOR").path("current").asInt());
        assertEquals("BRONZE", badge(leader, "CONNECTOR").path("tier").asString());
        assertTrue(badge(leader, "CONNECTOR").path("currentTier").isNull());
        assertEquals(1, badge(leader, "FIRST_MOVE").path("current").asInt());
        assertEquals(1, badge(leader, "FOUNDER").path("current").asInt());
    }

    @Test
    void savedTiersAreOnlyRaisedEvenWhenRequestsRaceAndAreRemovedWithTheStudent() throws Exception {
        var people = people();
        connect(people);
        statuses(race(() -> call("GET", "/achievements/me", people.leaderToken(), null),
                () -> call("GET", "/achievements/me", people.leaderToken(), null)), 200, 200);
        assertEquals(1, count("select count(*) from earned_badges where student_id=? and badge='CONNECTOR'", people.leader()));
        database.update("update earned_badges set best_tier='GOLD' where student_id=? and badge='CONNECTOR'", people.leader());

        // A live Bronze must not overwrite the saved Gold.
        JsonNode leader = standing(people.leaderToken());
        assertEquals("GOLD", badge(leader, "CONNECTOR").path("tier").asString());
        assertEquals("BRONZE", badge(leader, "CONNECTOR").path("currentTier").asString());
        assertEquals("GOLD", database.queryForObject(
                "select best_tier from earned_badges where student_id=? and badge='CONNECTOR'", String.class, people.leader()));

        account("admin@example.test", "ADMIN");
        expect(call("DELETE", "/admin/users/" + people.leader(), login("admin@example.test"), null), 204);
        assertEquals(0, count("select count(*) from earned_badges where student_id=?", people.leader()));
    }

    @Test
    void bothGroupAndBuddySchedulesCountOnlyOccurrencesWithinCurrentParticipation() {
        var people = people();
        long group = group(people.leaderToken(), 3);
        expect(approve(group, apply(group, people.memberToken()), people.leaderToken()), 200);
        long connection = connect(people);
        LocalDateTime upcoming = LocalDateTime.now(ZONE).plusDays(1).withSecond(0).withNano(0);
        // Each series has two old occurrences and one future one. Both current tenures began today.
        plan("/groups/" + group, people.leaderToken(), upcoming.minusWeeks(2), 3);
        plan("/connections/" + connection, people.memberToken(), upcoming.minusWeeks(2), 3);

        assertEquals(2, badge(standing(people.memberToken()), "PLANNER").path("current").asInt());
        assertEquals(2, badge(standing(people.leaderToken()), "PLANNER").path("current").asInt());
    }

    @Test
    void cancellationRemovalAndReadmissionRecomputeTheAvailablePlanningHistory() {
        var people = people();
        long group = group(people.leaderToken(), 3);
        expect(approve(group, apply(group, people.memberToken()), people.leaderToken()), 200);
        LocalDateTime upcoming = LocalDateTime.now(ZONE).plusDays(1).withSecond(0).withNano(0);
        database.update("update group_memberships set joined_at=now()-interval '4 weeks' where study_group_id=? and student_id=?", group, people.member());
        long session = plan("/groups/" + group, people.leaderToken(), upcoming.minusWeeks(2), 3);
        assertEquals(3, badge(standing(people.memberToken()), "PLANNER").path("current").asInt());

        expect(call("DELETE", "/groups/" + group + "/members/" + people.member(), people.leaderToken(), null), 204);
        assertEquals(0, badge(standing(people.memberToken()), "PLANNER").path("current").asInt());
        assertEquals(0, standing(people.memberToken()).path("streak").path("bestWeeks").asInt());
        expect(approve(group, apply(group, people.memberToken()), people.leaderToken()), 200);
        assertEquals(1, badge(standing(people.memberToken()), "PLANNER").path("current").asInt());

        expect(call("POST", "/sessions/" + session + "/cancel", people.leaderToken(), Map.of("expectedVersion", 0)), 200);
        assertEquals(0, badge(standing(people.memberToken()), "PLANNER").path("current").asInt());
    }

    @Test
    void closedGroupsEndedConnectionsAndInactiveBuddiesCannotSupplyPlannedOccurrences() {
        var people = people();
        long group = group(people.leaderToken(), 3);
        expect(approve(group, apply(group, people.memberToken()), people.leaderToken()), 200);
        long connection = connect(people);
        LocalDateTime upcoming = LocalDateTime.now(ZONE).plusDays(1).withSecond(0).withNano(0);
        plan("/groups/" + group, people.leaderToken(), upcoming, 1);
        plan("/connections/" + connection, people.memberToken(), upcoming, 1);
        assertEquals(2, badge(standing(people.memberToken()), "PLANNER").path("current").asInt());

        // Direct fixture changes check the query's access predicate independently of automatic cancellation.
        database.update("update study_groups set active=false where id=?", group);
        assertEquals(1, badge(standing(people.memberToken()), "PLANNER").path("current").asInt());
        database.update("update users set active=false where id=?", people.leader());
        assertEquals(0, badge(standing(people.memberToken()), "PLANNER").path("current").asInt());
        database.update("update users set active=true where id=?", people.leader());
        database.update("update connections set ended_at=now() where id=?", connection);
        assertEquals(0, badge(standing(people.memberToken()), "PLANNER").path("current").asInt());
    }

    private JsonNode standing(String token) { return expect(call("GET", "/achievements/me", token, null), 200); }

    private JsonNode badge(JsonNode standing, String key) {
        for (JsonNode badge : standing.path("badges")) if (key.equals(badge.path("key").asString())) return badge;
        throw new AssertionError("Missing badge " + key);
    }

    private long plan(String parentPath, String token, LocalDateTime start, int count) {
        return expect(call("POST", parentPath + "/sessions", token,
                Map.of("title", "Planned revision", "startLocal", start.toString(), "endLocal", start.plusHours(1).toString(),
                        "timeZone", ZONE.getId(), "occurrenceCount", count)), 201).path("id").asLong();
    }

    private long connect(People people) {
        expect(call("POST", "/match-requests/" + send(people.member(), people.leaderToken()) + "/accept", people.memberToken(), null), 200);
        return database.queryForObject("select id from connections where ended_at is null", Long.class);
    }

    private People people() {
        long leader = student("leader@example.test", "Leader", "91234563");
        long member = student("member@example.test", "Member", "91234564");
        return new People(leader, member, login("leader@example.test"), login("member@example.test"));
    }

    private record People(long leader, long member, String leaderToken, String memberToken) { }
}
