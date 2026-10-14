package com.studybuddy.integration;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

class StudySessionHttpTest extends PostgresHttpTest {
    @Test void groupLeaderSchedulesMembersReadAndExportAndOutsidersCannotAccess() {
        var people = people();
        long group = group(people.leaderToken, 3);
        long request = apply(group, people.memberToken);
        expect(approve(group, request, people.leaderToken), 200);
        expect(call("POST", "/groups/" + group + "/sessions", people.memberToken, details("Revision", 1)), 403);
        JsonNode created = expect(call("POST", "/groups/" + group + "/sessions", people.leaderToken, details("Revision", 2)), 201);
        long id = created.path("id").asLong();
        assertTrue(created.path("canManage").asBoolean());
        assertEquals("GROUP", created.path("scope").asString());
        JsonNode member = expect(call("GET", "/sessions/" + id, people.memberToken, null), 200);
        assertFalse(member.path("canManage").asBoolean());
        assertEquals(2, member.path("occurrences").size());
        expect(call("GET", "/sessions/" + id, people.outsiderToken, null), 403);
        expect(call("GET", "/sessions/" + id + "/calendar", people.outsiderToken, null), 403);
        expect(call("GET", "/groups/" + group + "/sessions/availability", people.outsiderToken, null), 403);
        assertEquals(1, expect(call("GET", "/sessions/mine", people.memberToken, null), 200).size());
        assertEquals(1, count("select count(*) from notifications where type='GROUP_SESSION_CREATED' and recipient_id=?", people.member));
        Reply calendar = call("GET", "/sessions/" + id + "/calendar", people.memberToken, null);
        assertEquals(200, calendar.status());
        assertTrue(calendar.contentType().startsWith("text/calendar"));
        assertEquals(2, calendar.body().split("BEGIN:VEVENT", -1).length - 1);
        assertFalse(calendar.body().contains("member@example.org"));
        assertFalse(calendar.body().contains("+6591234567"));
        expect(call("GET", "/sessions/" + id, null, null), 401);
    }
    @Test void groupEditCancelRejectStaleRevisionsAndEmitOnlySuccessfulNotifications() {
        var people = people(); long group = acceptedGroup(people);
        JsonNode created = expect(call("POST", "/groups/" + group + "/sessions", people.leaderToken, details("Revision", 1)), 201);
        long id = created.path("id").asLong();
        var update = details("Exam revision", 1); update.put("expectedVersion", created.path("version").asLong());
        JsonNode edited = expect(call("PUT", "/sessions/" + id, people.leaderToken, update), 200);
        assertEquals(1, edited.path("version").asLong());
        expect(call("PUT", "/sessions/" + id, people.leaderToken, update), 409);
        expect(call("POST", "/sessions/" + id + "/cancel", people.memberToken, Map.of("expectedVersion", 1)), 403);
        expect(call("POST", "/sessions/" + id + "/cancel", people.leaderToken, Map.of("expectedVersion", 0)), 409);
        JsonNode cancelled = expect(call("POST", "/sessions/" + id + "/cancel", people.leaderToken, Map.of("expectedVersion", 1)), 200);
        assertEquals("CANCELLED", cancelled.path("status").asString());
        assertTrue(cancelled.path("nextOccurrence").isNull());
        assertEquals(0, expect(call("GET", "/sessions/mine", people.memberToken, null), 200).size());
        assertTrue(call("GET", "/sessions/" + id + "/calendar", people.memberToken, null).body().contains("STATUS:CANCELLED"));
        update.put("expectedVersion", cancelled.path("version").asLong());
        expect(call("PUT", "/sessions/" + id, people.leaderToken, update), 409);
        assertEquals(1, count("select count(*) from notifications where type='GROUP_SESSION_UPDATED'"));
        assertEquals(1, count("select count(*) from notifications where type='GROUP_SESSION_CANCELLED'"));
    }
    @Test void buddySessionsBindToExactConnectionAndEitherBuddyCanManage() {
        var people = people(); long connection = connection(people);
        JsonNode created = expect(call("POST", "/connections/" + connection + "/sessions", people.leaderToken, details("Buddy revision", 1)), 201);
        long id = created.path("id").asLong();
        assertEquals("BUDDY", created.path("scope").asString());
        assertTrue(expect(call("GET", "/sessions/" + id, people.memberToken, null), 200).path("canManage").asBoolean());
        var update = details("Buddy final revision", 1); update.put("expectedVersion", 0);
        expect(call("PUT", "/sessions/" + id, people.memberToken, update), 200);
        expect(call("POST", "/connections/" + connection + "/sessions", people.outsiderToken, details("Intrusion", 1)), 403);
        expect(call("DELETE", "/connections/" + connection, people.memberToken, null), 204);
        assertEquals("CANCELLED", database.queryForObject("select status from study_sessions where id=?", String.class, id));
        expect(call("GET", "/sessions/" + id, people.leaderToken, null), 403);
        expect(call("GET", "/sessions/" + id + "/calendar", people.memberToken, null), 403);
        long reconnectRequest = send(people.member, people.leaderToken);
        expect(call("POST", "/match-requests/" + reconnectRequest + "/accept", people.memberToken, null), 200);
        expect(call("GET", "/sessions/" + id, people.memberToken, null), 403);
        assertEquals(0, expect(call("GET", "/sessions/mine", people.leaderToken, null), 200).size());
        assertEquals(2, count("select count(*) from notifications where type='BUDDY_SESSION_CANCELLED'"));
    }
    @Test void sharedAvailabilityIntersectsEveryCurrentMemberAndRemovalRevokesSessionAccess() {
        var people = people(); long group = acceptedGroup(people);
        database.update("update availability_slots set start_time='10:00',end_time='12:00' where student_id=?", people.member);
        JsonNode shared = expect(call("GET", "/groups/" + group + "/sessions/availability", people.memberToken, null), 200);
        assertEquals(2, shared.path("participantCount").asInt());
        assertEquals("Asia/Singapore", shared.path("timeZone").asString());
        assertEquals(60, shared.path("sharedMinutes").asLong());
        assertEquals("10:00:00", shared.path("sharedSlots").get(0).path("startTime").asString());
        JsonNode created = expect(call("POST", "/groups/" + group + "/sessions", people.leaderToken, details("Revision", 1)), 201);
        expect(call("DELETE", "/groups/" + group + "/members/" + people.member, people.leaderToken, null), 204);
        expect(call("GET", "/sessions/" + created.path("id").asLong(), people.memberToken, null), 403);
        assertEquals(0, expect(call("GET", "/sessions/mine", people.memberToken, null), 200).size());
        assertEquals(120, expect(call("GET", "/groups/" + group + "/sessions/availability", people.leaderToken, null), 200).path("sharedMinutes").asLong());
    }
    @Test void concurrentEditsAndCloseSerializeWithoutLostSessionUpdates() throws Exception {
        var people = people(); long group = acceptedGroup(people);
        long id = expect(call("POST", "/groups/" + group + "/sessions", people.leaderToken, details("Revision", 1)), 201).path("id").asLong();
        var one = details("First", 1); one.put("expectedVersion", 0);
        var two = details("Second", 1); two.put("expectedVersion", 0);
        statuses(race(() -> call("PUT", "/sessions/" + id, people.leaderToken, one),
            () -> call("PUT", "/sessions/" + id, people.leaderToken, two)), 200, 409);
        assertEquals(1, count("select count(*) from notifications where type='GROUP_SESSION_UPDATED'"));
        expect(call("POST", "/groups/" + group + "/close", people.leaderToken, null), 200);
        assertEquals("CANCELLED", database.queryForObject("select status from study_sessions where id=?", String.class, id));
        expect(call("GET", "/sessions/" + id, people.memberToken, null), 403);
        expect(call("POST", "/groups/" + group + "/sessions", people.leaderToken, details("Too late", 1)), 403);
    }
    @Test void invalidDatesLinksRecurrencesAndFailedEventsLeaveNoSavedSession() {
        var people = people(); long group = acceptedGroup(people);
        var invalid = details("Revision", 53);
        expect(call("POST", "/groups/" + group + "/sessions", people.leaderToken, invalid), 400);
        invalid = details("Revision", 1); invalid.put("roomLink", "https://meet.example.org/?token=secret");
        expect(call("POST", "/groups/" + group + "/sessions", people.leaderToken, invalid), 400);
        invalid = details("Revision", 1); invalid.put("startLocal", "2026-11-01T01:30"); invalid.put("endLocal", "2026-11-01T02:30"); invalid.put("timeZone", "America/New_York");
        assertTrue(expect(call("POST", "/groups/" + group + "/sessions", people.leaderToken, invalid), 400).path("fieldErrors").has("startLocal"));
        assertEquals(0, count("select count(*) from study_sessions"));
        database.execute("alter table notifications add constraint test_notification_failure check(false) not valid");
        expect(call("POST", "/groups/" + group + "/sessions", people.leaderToken, details("Rollback", 1)), 409);
        assertEquals(0, count("select count(*) from study_sessions"));
    }
    @Test void deactivationAndPermanentDeletionCancelOrCleanDependencies() {
        var people = people(); long group = acceptedGroup(people); long connection = connection(people);
        long groupSession = expect(call("POST", "/groups/" + group + "/sessions", people.leaderToken, details("Group", 1)), 201).path("id").asLong();
        long buddySession = expect(call("POST", "/connections/" + connection + "/sessions", people.memberToken, details("Buddy", 1)), 201).path("id").asLong();
        account("admin@example.org", "ADMIN"); String admin = login("admin@example.org");
        expect(call("POST", "/admin/users/" + people.leader + "/deactivate", admin, null), 200);
        assertEquals(2, count("select count(*) from study_sessions where status='CANCELLED'"));
        expect(call("GET", "/sessions/" + groupSession, people.memberToken, null), 403);
        expect(call("GET", "/sessions/" + buddySession, people.memberToken, null), 403);
        expect(call("DELETE", "/admin/users/" + people.leader, admin, null), 204);
        assertEquals(0, count("select count(*) from study_sessions"));
        assertEquals(0, count("select count(*) from notifications where resource_type='SESSION'"));
    }
    @Test void weeklyPersonalAndGroupClockTimesRoundTripThroughJdbcWithoutTimezoneShift() {
        var people = people();
        JsonNode personal = expect(call("PUT", "/profile/me/availability", people.leaderToken, Map.of("slots", List.of(
            Map.of("dayOfWeek", "MONDAY", "startTime", "10:00", "endTime", "11:15")))), 200);
        assertEquals("10:00:00", personal.get(0).path("startTime").asString());
        assertEquals("10:00:00", database.queryForObject("select start_time::text from availability_slots where student_id=?", String.class, people.leader));
        assertEquals("10:00:00", expect(call("GET", "/profile/me", people.leaderToken, null), 200).path("availability").get(0).path("startTime").asString());
        long group = group(people.leaderToken, 3);
        assertEquals("09:00:00", database.queryForObject("select start_time::text from group_availability_slots where study_group_id=?", String.class, group));
        assertEquals("09:00:00", expect(call("GET", "/groups/" + group, people.leaderToken, null), 200).path("availability").get(0).path("startTime").asString());
        database.update("update group_availability_slots set start_time='09:30',end_time='11:30' where study_group_id=?", group);
        assertEquals("09:30:00", expect(call("GET", "/groups/" + group, people.leaderToken, null), 200).path("availability").get(0).path("startTime").asString());
    }
    private long acceptedGroup(People people) {
        long group = group(people.leaderToken, 3); expect(approve(group, apply(group, people.memberToken), people.leaderToken), 200); return group;
    }
    private long connection(People people) {
        expect(call("POST", "/match-requests/" + send(people.member, people.leaderToken) + "/accept", people.memberToken, null), 200);
        return database.queryForObject("select id from connections where ended_at is null and student_a_id=? and student_b_id=?", Long.class, people.leader, people.member);
    }
    private People people() {
        long leader = student("leader@example.org", "Leader", "+6591234567");
        long member = student("member@example.org", "Member", "+6592345678");
        student("outsider@example.org", "Outsider", "+6593456789");
        return new People(leader, member, login("leader@example.org"), login("member@example.org"), login("outsider@example.org"));
    }
    private HashMap<String,Object> details(String title, int count) {
        LocalDateTime start = LocalDateTime.now(ZoneOffset.UTC).plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        return new HashMap<>(Map.of("title", title, "startLocal", start.toString(), "endLocal", start.plusHours(1).toString(), "timeZone", "Asia/Singapore", "occurrenceCount", count, "agenda", "Review notes", "location", "Library"));
    }
    private record People(long leader, long member, String leaderToken, String memberToken, String outsiderToken) { }
}
