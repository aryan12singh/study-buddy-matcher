package com.studybuddy.integration;

import com.studybuddy.ai.AiClient;
import com.studybuddy.ai.AiUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Session roadmaps over the real web, security and database stack, with Gemini replaced by a fake. */
@Import(SessionStudyPlanHttpTest.FakeAiConfiguration.class)
class SessionStudyPlanHttpTest extends PostgresHttpTest {
    /** Fits two one-hour meetings. */
    private static final String TWO_MEETING_REPLY = """
            {"sourceTopics": ["Polymorphism", "Interfaces"],
             "entries": [{"sequence": 1, "title": "Polymorphism", "plannedMinutes": 45, "tasks": ["Walk through slide 12"]},
                         {"sequence": 2, "title": "Interfaces", "plannedMinutes": 50, "tasks": []}]}""";

    private static volatile String reply;
    private static final List<String> PROMPTS = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class FakeAiConfiguration {
        @Bean
        @Primary
        AiClient fakeAi() {
            return (studentId, prompt, attachments) -> {
                PROMPTS.add(prompt);
                if (reply == null) throw new AiUnavailableException();
                return reply;
            };
        }
    }

    @LocalServerPort private int port;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @BeforeEach
    void resetFakeAi() {
        reply = TWO_MEETING_REPLY;
        PROMPTS.clear();
    }

    private record People(long leader, long member, String leaderToken, String memberToken, String outsiderToken) { }

    private People people() {
        long leader = student("leader@example.org", "Leader", "+6591234567");
        long member = student("member@example.org", "Member", "+6592345678");
        student("outsider@example.org", "Outsider", "+6593456789");
        return new People(leader, member, login("leader@example.org"), login("member@example.org"), login("outsider@example.org"));
    }

    private long acceptedGroup(People people) {
        long group = group(people.leaderToken(), 3);
        expect(approve(group, apply(group, people.memberToken()), people.leaderToken()), 200);
        return group;
    }

    private long connection(People people) {
        expect(call("POST", "/match-requests/" + send(people.member(), people.leaderToken()) + "/accept", people.memberToken(), null), 200);
        return database.queryForObject("select id from connections where ended_at is null and student_a_id=? and student_b_id=?",
                Long.class, people.leader(), people.member());
    }

    /** One-hour weekly meetings at 10:00 in Singapore, starting in two days. */
    private static HashMap<String, Object> details(int weeks) {
        LocalDateTime start = LocalDateTime.now(ZoneId.of("Asia/Singapore")).plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        return new HashMap<>(Map.of("title", "Revision", "startLocal", start.toString(), "endLocal", start.plusHours(1).toString(),
                "timeZone", "Asia/Singapore", "occurrenceCount", weeks));
    }

    private long session(String path, String token, int weeks) {
        return expect(call("POST", path, token, details(weeks)), 201).path("id").asLong();
    }

    /** A multipart POST to the session's study plan with form fields only (pasted topics). */
    private Reply planSession(long session, String token, Map<String, Object> fields) {
        String boundary = "----sessionplan" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        fields.forEach((name, value) -> body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\""
                + name + "\"\r\n\r\n" + value + "\r\n").getBytes(StandardCharsets.UTF_8)));
        body.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/sessions/" + session + "/study-plan"))
                    .timeout(Duration.ofSeconds(20)).header("Accept", "application/json").header("Authorization", "Bearer " + token)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode payload = response.body().isBlank() ? json.nullNode() : json.readTree(response.body());
            return new Reply(response.statusCode(), payload, response.body(), response.headers().firstValue("content-type").orElse(""));
        } catch (Exception error) {
            throw new AssertionError("Multipart request failed", error);
        }
    }

    private static final Map<String, Object> TOPICS = Map.of("topicsText", "Polymorphism\nInterfaces", "studyGoal", "CONCEPT_REVIEW");

    @Test
    void theLeaderPlansAWeeklyGroupSessionMembersReadItAndOutsidersCannotSeeIt() {
        People people = people();
        long group = acceptedGroup(people);
        long session = session("/groups/" + group + "/sessions", people.leaderToken(), 2);

        // No roadmap yet is a normal state, not an error.
        Reply none = call("GET", "/sessions/" + session + "/study-plan", people.memberToken(), null);
        assertEquals(204, none.status(), none.body());
        assertTrue(none.body().isEmpty());
        expect(planSession(session, people.memberToken(), TOPICS), 403);
        assertTrue(PROMPTS.isEmpty(), "A member who may not plan must not spend an AI call");

        JsonNode plan = expect(planSession(session, people.leaderToken(), TOPICS), 201);
        assertEquals("SESSION", plan.path("scope").asString());
        assertEquals(session, plan.path("sessionId").asLong());
        assertEquals("IS442", plan.path("course").path("code").asString());
        assertEquals(2, plan.path("blockCount").asInt());
        assertEquals(60, plan.path("minutesPerBlock").asInt());
        assertEquals(details(2).get("startLocal").toString().substring(0, 10), plan.path("firstBlockDate").asString());
        assertTrue(plan.path("canManage").asBoolean());
        assertTrue(plan.path("examDate").isNull());
        assertTrue(PROMPTS.getFirst().contains("Meeting 1 on"), PROMPTS.getFirst());
        assertTrue(PROMPTS.getFirst().contains("up to 60 minutes"), PROMPTS.getFirst());

        JsonNode read = expect(call("GET", "/sessions/" + session + "/study-plan", people.memberToken(), null), 200);
        assertFalse(read.path("canManage").asBoolean());
        assertEquals(2, read.path("entries").size());
        long planId = plan.path("id").asLong();
        long entry = plan.path("entries").get(0).path("id").asLong();
        assertFalse(expect(call("GET", "/study-plans/" + planId, people.memberToken(), null), 200).path("canManage").asBoolean());
        expect(call("PUT", "/study-plans/" + planId + "/entries/" + entry + "/progress", people.memberToken(), Map.of("completed", true)), 403);
        expect(call("DELETE", "/study-plans/" + planId, people.memberToken(), null), 403);

        expect(call("GET", "/sessions/" + session + "/study-plan", people.outsiderToken(), null), 404);
        expect(call("GET", "/study-plans/" + planId, people.outsiderToken(), null), 404);
        expect(planSession(session, people.outsiderToken(), TOPICS), 404);
        expect(call("PUT", "/study-plans/" + planId + "/entries/" + entry + "/progress", people.outsiderToken(), Map.of("completed", true)), 404);

        JsonNode ticked = expect(call("PUT", "/study-plans/" + planId + "/entries/" + entry + "/progress", people.leaderToken(),
                Map.of("completed", true)), 200);
        assertTrue(ticked.path("entries").get(0).path("completed").asBoolean());
        assertTrue(expect(call("GET", "/sessions/" + session + "/study-plan", people.memberToken(), null), 200)
                .path("entries").get(0).path("completed").asBoolean());
        assertEquals(0, expect(call("GET", "/study-plans", people.leaderToken(), null), 200).size(),
                "A session plan is not one of the leader's personal plans");
    }

    @Test
    void aNewRoadmapReplacesTheSessionsOldOne() {
        People people = people();
        long session = session("/groups/" + acceptedGroup(people) + "/sessions", people.leaderToken(), 2);
        long first = expect(planSession(session, people.leaderToken(), TOPICS), 201).path("id").asLong();

        long second = expect(planSession(session, people.leaderToken(), TOPICS), 201).path("id").asLong();

        assertEquals(1, count("select count(*) from study_plans where session_id=?", session));
        expect(call("GET", "/study-plans/" + first, people.leaderToken(), null), 404);
        assertEquals(second, expect(call("GET", "/sessions/" + session + "/study-plan", people.memberToken(), null), 200).path("id").asLong());
    }

    @Test
    void aCancelledSessionHasNothingToPlanAndAFailedAiCallKeepsTheOldRoadmap() {
        People people = people();
        long group = acceptedGroup(people);
        long session = session("/groups/" + group + "/sessions", people.leaderToken(), 2);
        long kept = expect(planSession(session, people.leaderToken(), TOPICS), 201).path("id").asLong();

        reply = null;
        JsonNode unavailable = expect(planSession(session, people.leaderToken(), TOPICS), 503);
        assertEquals("AI_UNAVAILABLE", unavailable.path("code").asString());
        assertEquals(kept, expect(call("GET", "/sessions/" + session + "/study-plan", people.leaderToken(), null), 200).path("id").asLong());

        reply = TWO_MEETING_REPLY;
        PROMPTS.clear();
        expect(call("POST", "/sessions/" + session + "/cancel", people.leaderToken(), Map.of("expectedVersion", 0)), 200);
        JsonNode cancelled = expect(planSession(session, people.leaderToken(), TOPICS), 409);
        assertEquals("NO_UPCOMING_MEETINGS", cancelled.path("code").asString());
        assertTrue(PROMPTS.isEmpty(), "Nothing to plan must not spend an AI call");
    }

    @Test
    void eitherBuddyPlansABuddySessionForTheCourseTheyChoose() {
        People people = people();
        long connection = connection(people);
        long session = session("/connections/" + connection + "/sessions", people.leaderToken(), 2);

        JsonNode noCourse = expect(planSession(session, people.memberToken(), TOPICS), 400);
        assertTrue(noCourse.path("fieldErrors").has("courseId"), noCourse.toString());
        expect(planSession(session, people.memberToken(), Map.of("courseId", 999999, "topicsText", "Polymorphism")), 400);
        assertTrue(PROMPTS.isEmpty());

        JsonNode plan = expect(planSession(session, people.memberToken(), Map.of("courseId", course, "topicsText", "Polymorphism")), 201);
        assertEquals(course, plan.path("course").path("id").asLong());
        assertTrue(expect(call("GET", "/sessions/" + session + "/study-plan", people.leaderToken(), null), 200).path("canManage").asBoolean());
        expect(call("GET", "/sessions/" + session + "/study-plan", people.outsiderToken(), null), 404);

        expect(call("DELETE", "/connections/" + connection, people.memberToken(), null), 204);
        expect(call("GET", "/sessions/" + session + "/study-plan", people.leaderToken(), null), 404);
    }
}
