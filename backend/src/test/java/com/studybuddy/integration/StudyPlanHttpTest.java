package com.studybuddy.integration;

import com.studybuddy.ai.AiAttachment;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The real web, security, validation and database stack, with Gemini replaced by a fake. */
@Import(StudyPlanHttpTest.FakeAiConfiguration.class)
class StudyPlanHttpTest extends PostgresHttpTest {
    private static final byte[] PDF = "%PDF-1.7\nWeek 4: Polymorphism".getBytes(StandardCharsets.US_ASCII);
    private static final String ONE_WEEK_REPLY = """
            {"sourceTopics": ["Polymorphism", "Interfaces"],
             "entries": [{"sequence": 1, "title": "Polymorphism", "plannedMinutes": 60, "tasks": ["Re-read the week 4 slides"]},
                         {"sequence": 1, "title": "Interfaces", "plannedMinutes": 45, "tasks": []}]}""";
    private static final String TWO_WEEK_REPLY = """
            {"sourceTopics": ["Polymorphism"],
             "entries": [{"sequence": 1, "title": "Polymorphism", "plannedMinutes": 60, "tasks": []},
                         {"sequence": 2, "title": "Revise polymorphism", "plannedMinutes": 60, "tasks": ["Past paper Q3"]}]}""";

    /** What the fake replies with; null makes it unavailable. Reset before each test. */
    private static volatile String reply;
    private static final List<List<AiAttachment>> AI_CALLS = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class FakeAiConfiguration {
        @Bean
        @Primary
        AiClient fakeAi() {
            return (studentId, prompt, attachments) -> {
                AI_CALLS.add(attachments);
                if (reply == null) {
                    throw new AiUnavailableException();
                }
                return reply;
            };
        }
    }

    @LocalServerPort private int port;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @BeforeEach
    void resetFakeAi() {
        reply = ONE_WEEK_REPLY;
        AI_CALLS.clear();
    }

    private record Upload(String filename, byte[] content) { }

    /** A multipart POST to /api/study-plans with form fields and files named "files". */
    private Reply createPlan(String token, Map<String, Object> fields, Upload... files) {
        String boundary = "----studyplan" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        fields.forEach((name, value) -> write(body, "--" + boundary + "\r\nContent-Disposition: form-data; name=\""
                + name + "\"\r\n\r\n" + value + "\r\n"));
        for (Upload file : files) {
            write(body, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"files\"; filename=\"" + file.filename()
                    + "\"\r\nContent-Type: application/octet-stream\r\n\r\n");
            body.writeBytes(file.content());
            write(body, "\r\n");
        }
        write(body, "--" + boundary + "--\r\n");
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/study-plans"))
                    .timeout(Duration.ofSeconds(20)).header("Accept", "application/json")
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()));
            if (token != null) {
                request.header("Authorization", "Bearer " + token);
            }
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode payload = response.body().isBlank() ? json.nullNode() : json.readTree(response.body());
            return new Reply(response.statusCode(), payload, response.body(), response.headers().firstValue("content-type").orElse(""));
        } catch (Exception error) {
            throw new AssertionError("Multipart request failed", error);
        }
    }

    private static void write(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, Object> thisWeek(int hoursPerWeek) {
        return Map.of("courseId", course, "hoursPerWeek", hoursPerWeek, "studyGoal", "EXAM_PREPARATION");
    }

    private static LocalDate todayInSingapore() {
        return LocalDate.now(ZoneId.of("Asia/Singapore"));
    }

    @Test
    void aStudentPlansThisWeekFromUploadedSlidesAndOnlyTheyCanSeeOrChangeIt() {
        student("priya@example.org", "Priya", "+6591234567");
        student("marcus@example.org", "Marcus", "+6598765432");
        String priya = login("priya@example.org");
        String marcus = login("marcus@example.org");

        JsonNode plan = expect(createPlan(priya, thisWeek(2), new Upload("week4.pdf", PDF)), 201);

        long id = plan.path("id").asLong();
        assertEquals("PERSONAL", plan.path("scope").asString());
        assertEquals("IS442", plan.path("course").path("code").asString());
        assertEquals(1, plan.path("blockCount").asInt());
        assertEquals(120, plan.path("minutesPerBlock").asInt());
        assertEquals(todayInSingapore().toString(), plan.path("firstBlockDate").asString());
        assertTrue(plan.path("examDate").isNull());
        assertEquals("UPLOAD", plan.path("sourceKind").asString());
        assertEquals("week4.pdf", plan.path("sourceFilenames").path(0).asString());
        assertEquals(2, plan.path("entries").size());
        assertEquals(todayInSingapore().toString(), plan.path("entries").path(0).path("scheduledDate").asString());
        assertTrue(plan.path("canManage").asBoolean());
        assertEquals(1, AI_CALLS.size());
        assertEquals("application/pdf", AI_CALLS.getFirst().getFirst().mediaType());
        String body = plan.toString();
        assertFalse(body.contains("priya@example.org") || body.contains("+6591234567"), body);

        JsonNode mine = expect(call("GET", "/study-plans", priya, null), 200);
        assertEquals(1, mine.size());
        assertEquals(2, mine.path(0).path("entryCount").asInt());
        assertEquals(0, mine.path(0).path("completedCount").asInt());
        assertEquals(0, expect(call("GET", "/study-plans", marcus, null), 200).size());

        long entry = plan.path("entries").path(0).path("id").asLong();
        expect(call("GET", "/study-plans/" + id, marcus, null), 404);
        expect(call("PUT", "/study-plans/" + id + "/entries/" + entry + "/progress", marcus, Map.of("completed", true)), 404);
        expect(call("DELETE", "/study-plans/" + id, marcus, null), 404);

        JsonNode ticked = expect(call("PUT", "/study-plans/" + id + "/entries/" + entry + "/progress", priya, Map.of("completed", true)), 200);
        assertTrue(ticked.path("entries").path(0).path("completed").asBoolean());
        assertEquals(1, expect(call("GET", "/study-plans", priya, null), 200).path(0).path("completedCount").asInt());
        expect(call("PUT", "/study-plans/" + id + "/entries/999999/progress", priya, Map.of("completed", true)), 404);
        expect(call("PUT", "/study-plans/" + id + "/entries/" + entry + "/progress", priya, Map.of()), 400);

        expect(call("DELETE", "/study-plans/" + id, priya, null), 204);
        expect(call("GET", "/study-plans/" + id, priya, null), 404);
        assertEquals(0, count("select count(*) from study_plan_entries"));
    }

    @Test
    void pastedTopicsWithAnExamPlanEveryWeekUntilIt() {
        student("priya@example.org", "Priya", "+6591234567");
        String priya = login("priya@example.org");
        reply = TWO_WEEK_REPLY;
        LocalDate exam = todayInSingapore().plusDays(10);

        JsonNode plan = expect(createPlan(priya, Map.of("courseId", course, "hoursPerWeek", 3,
                "examDate", exam.toString(), "topicsText", "Polymorphism")), 201);

        assertEquals(2, plan.path("blockCount").asInt());
        assertEquals(exam.toString(), plan.path("examDate").asString());
        assertEquals("PASTE", plan.path("sourceKind").asString());
        assertEquals(0, plan.path("sourceFilenames").size());
        assertEquals(todayInSingapore().plusDays(7).toString(), plan.path("entries").path(1).path("scheduledDate").asString());
        assertTrue(AI_CALLS.getFirst().isEmpty());
    }

    @Test
    void aStudentMayKeepSeveralPlansForOneCourse() {
        student("priya@example.org", "Priya", "+6591234567");
        String priya = login("priya@example.org");

        expect(createPlan(priya, thisWeek(2), new Upload("week4.pdf", PDF)), 201);
        expect(createPlan(priya, thisWeek(2), new Upload("week5.pdf", PDF)), 201);

        assertEquals(2, expect(call("GET", "/study-plans", priya, null), 200).size());
    }

    @Test
    void whenTheAiIsUnavailableNothingIsSaved() {
        student("priya@example.org", "Priya", "+6591234567");
        String priya = login("priya@example.org");

        reply = null;
        assertEquals("AI_UNAVAILABLE", expect(createPlan(priya, thisWeek(2), new Upload("week4.pdf", PDF)), 503).path("code").asString());
        reply = "{\"sourceTopics\": [], \"entries\": []}";
        assertEquals("AI_UNAVAILABLE", expect(createPlan(priya, thisWeek(2), new Upload("week4.pdf", PDF)), 503).path("code").asString());

        assertEquals(0, count("select count(*) from study_plans"));
    }

    @Test
    void badRequestsAreRefusedBeforeTheAiIsAsked() {
        student("priya@example.org", "Priya", "+6591234567");
        String priya = login("priya@example.org");
        Upload slides = new Upload("week4.pdf", PDF);

        expect(createPlan(priya, Map.of("hoursPerWeek", 2), slides), 400);
        expect(createPlan(priya, Map.of("courseId", course), slides), 400);
        expect(createPlan(priya, Map.of("courseId", course, "hoursPerWeek", "two"), slides), 400);
        assertTrue(expect(createPlan(priya, Map.of("courseId", 999999, "hoursPerWeek", 2), slides), 400)
                .path("fieldErrors").has("courseId"));
        assertTrue(expect(createPlan(priya, Map.of("courseId", course, "hoursPerWeek", 2, "examDate",
                todayInSingapore().toString()), slides), 400).path("fieldErrors").has("examDate"));
        assertTrue(expect(createPlan(priya, thisWeek(41), slides), 400).path("fieldErrors").has("hoursPerWeek"));
        expect(createPlan(priya, thisWeek(2)), 400);
        expect(createPlan(priya, Map.of("courseId", course, "hoursPerWeek", 2, "topicsText", "Polymorphism"), slides), 400);
        expect(createPlan(priya, thisWeek(2), new Upload("slides.pptx", "PK".getBytes())), 415);
        expect(createPlan(priya, thisWeek(2), new Upload("week4.pdf", "not a pdf".getBytes())), 415);

        assertTrue(AI_CALLS.isEmpty());
        assertEquals(0, count("select count(*) from study_plans"));
    }

    @Test
    void onlySignedInStudentsCanPlan() {
        account("admin@example.org", "ADMIN");
        String admin = login("admin@example.org");

        expect(createPlan(null, thisWeek(2), new Upload("week4.pdf", PDF)), 401);
        expect(createPlan(admin, thisWeek(2), new Upload("week4.pdf", PDF)), 403);
        expect(call("GET", "/study-plans", admin, null), 403);
        assertTrue(AI_CALLS.isEmpty());
    }
}
