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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The real web, security, validation and database stack for group and personal quizzes, with Gemini replaced by a fake. */
@Import(QuizHttpTest.FakeAiConfiguration.class)
class QuizHttpTest extends PostgresHttpTest {
    /** application.yml asks for this many questions per quiz. */
    private static final int QUESTIONS = 10;
    /** Question i's correct option is i % 4. */
    private static final String TEN_QUESTIONS = "{\"questions\": [" + IntStream.range(0, QUESTIONS)
            .mapToObj(i -> "{\"prompt\": \"Question " + i + "\", \"options\": [\"A\", \"B\", \"C\", \"D\"], \"correctOption\": "
                    + (i % 4) + ", \"explanation\": \"Because " + i + "\"}")
            .collect(Collectors.joining(", ")) + "]}";

    private static final byte[] PDF = "%PDF-1.7\nWeek 4: Polymorphism".getBytes(StandardCharsets.US_ASCII);
    private static final Map<String, Object> NOTES = Map.of("notesText", "Polymorphism lets a subclass override a method.");

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
    private String leader;
    private String member;
    private String latecomer;
    private String outsider;
    private long leaderId;
    private long memberId;
    private long groupId;

    @BeforeEach
    void aGroupOfThreeAndAnOutsider() {
        reply = TEN_QUESTIONS;
        AI_CALLS.clear();
        leaderId = student("lead@example.org", "Lena", "+6591110001");
        memberId = student("member@example.org", "Marcus", "+6591110002");
        student("late@example.org", "Lin", "+6591110003");
        student("outsider@example.org", "Omar", "+6591110004");
        leader = login("lead@example.org");
        member = login("member@example.org");
        latecomer = login("late@example.org");
        outsider = login("outsider@example.org");
        groupId = group(leader, 5);
        expect(approve(groupId, apply(groupId, member), leader), 200);
        expect(approve(groupId, apply(groupId, latecomer), leader), 200);
    }

    private record Upload(String filename, byte[] content) { }

    /** A multipart POST with form fields and files named "files", as the quiz bot sends it. */
    private Reply create(String path, String token, Map<String, Object> fields, Upload... files) {
        String boundary = "----quiz" + UUID.randomUUID();
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
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api" + path))
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

    private Reply createGroupQuiz(String token, Map<String, Object> fields, Upload... files) {
        return create("/groups/" + groupId + "/quizzes", token, fields, files);
    }

    private long startQuiz(String token) {
        return expect(createGroupQuiz(token, NOTES), 201).path("id").asLong();
    }

    /** Answers in question order: the first {@code correct} questions right, the rest wrong. */
    private List<Map<String, Object>> answers(JsonNode questions, int correct) {
        List<Map<String, Object>> answers = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            int right = i % 4;
            answers.add(Map.of("questionId", questions.get(i).path("id").asLong(), "optionIndex", i < correct ? right : (right + 1) % 4));
        }
        return answers;
    }

    private Reply submit(long quizId, String token, List<Map<String, Object>> answers) {
        return call("POST", "/quizzes/" + quizId + "/attempts", token, Map.of("answers", answers));
    }

    private static void conflict(Reply reply, String code) {
        assertEquals(409, reply.status(), reply.body());
        assertEquals(code, reply.payload().path("code").asString());
    }

    @Test
    void theGroupTakesAQuizAndIsRankedByScore() {
        long quizId = startQuiz(leader);

        JsonNode listed = expect(call("GET", "/groups/" + groupId + "/quizzes", member, null), 200);
        assertEquals(1, listed.size());
        assertEquals("OPEN", listed.get(0).path("status").asString());
        assertEquals("NOT_STARTED", listed.get(0).path("myAttempt").asString());
        assertEquals(QUESTIONS, listed.get(0).path("questionCount").asInt());
        assertFalse(listed.get(0).path("canHost").asBoolean());

        Reply started = call("POST", "/quizzes/" + quizId + "/start", member, null);
        JsonNode questions = expect(started, 200).path("questions");
        assertEquals(QUESTIONS, questions.size());
        assertFalse(started.body().contains("correctOption"), "Answers never reach the browser while the quiz is open");
        assertFalse(started.body().contains("explanation"));
        String startedAt = started.payload().path("startedAt").asString();
        assertEquals(startedAt, expect(call("POST", "/quizzes/" + quizId + "/start", member, null), 200)
                .path("startedAt").asString(), "Starting again keeps the original start time");

        JsonNode memberResult = expect(submit(quizId, member, answers(questions, QUESTIONS)), 201);
        assertEquals(QUESTIONS, memberResult.path("score").asInt());
        assertEquals(1, memberResult.path("rank").asInt());
        conflict(submit(quizId, member, answers(questions, 0)), "ALREADY_SUBMITTED");
        conflict(call("POST", "/quizzes/" + quizId + "/start", member, null), "ALREADY_SUBMITTED");

        JsonNode leaderQuestions = expect(call("POST", "/quizzes/" + quizId + "/start", leader, null), 200).path("questions");
        JsonNode leaderResult = expect(submit(quizId, leader, answers(leaderQuestions, 6)), 201);
        assertEquals(6, leaderResult.path("score").asInt());
        assertEquals(2, leaderResult.path("rank").asInt());

        Reply board = call("GET", "/quizzes/" + quizId + "/leaderboard", latecomer, null);
        JsonNode leaderboard = expect(board, 200);
        assertEquals(2, leaderboard.path("entries").size());
        assertEquals(1, leaderboard.path("notSubmittedCount").asInt());
        assertEquals(memberId, leaderboard.path("entries").get(0).path("studentId").asLong());
        assertEquals("Marcus", leaderboard.path("entries").get(0).path("displayName").asString());
        assertFalse(leaderboard.path("entries").get(0).path("me").asBoolean());
        assertFalse(board.body().contains("@example.org"), "No emails on the leaderboard");
        assertFalse(board.body().contains("+659111"), "No contact numbers on the leaderboard");

        JsonNode mine = expect(call("GET", "/quizzes/" + quizId, member, null), 200);
        assertEquals("SUBMITTED", mine.path("myAttempt").asString());
        assertEquals(2, mine.path("submittedCount").asInt());

        // A removed member keeps their attempt as history, but neither count shows it any more.
        expect(call("DELETE", "/groups/" + groupId + "/members/" + memberId, leader, null), 204);
        assertEquals(1, expect(call("GET", "/quizzes/" + quizId, leader, null), 200).path("submittedCount").asInt());
        assertEquals(1, expect(call("GET", "/quizzes/" + quizId + "/leaderboard", leader, null), 200).path("entries").size());
        assertEquals(1, expect(call("GET", "/groups/" + groupId + "/quizzes", leader, null), 200).get(0).path("submittedCount").asInt());
    }

    @Test
    void answersAreRevealedOnlyAfterTheHostClosesTheQuiz() {
        long quizId = startQuiz(leader);
        JsonNode questions = expect(call("POST", "/quizzes/" + quizId + "/start", member, null), 200).path("questions");
        expect(submit(quizId, member, answers(questions, 3)), 201);

        conflict(call("GET", "/quizzes/" + quizId + "/review", member, null), "QUIZ_NOT_CLOSED");
        expect(call("POST", "/quizzes/" + quizId + "/close", member, null), 403);
        assertEquals("CLOSED", expect(call("POST", "/quizzes/" + quizId + "/close", leader, null), 200).path("status").asString());

        conflict(call("POST", "/quizzes/" + quizId + "/start", latecomer, null), "QUIZ_CLOSED");
        JsonNode review = expect(call("GET", "/quizzes/" + quizId + "/review", member, null), 200);
        assertEquals(3, review.path("score").asInt());
        JsonNode first = review.path("questions").get(0);
        assertEquals(0, first.path("correctOption").asInt());
        assertEquals(0, first.path("chosenOption").asInt());
        assertEquals("Because 0", first.path("explanation").asString());

        JsonNode latecomerReview = expect(call("GET", "/quizzes/" + quizId + "/review", latecomer, null), 200);
        assertTrue(latecomerReview.path("score").isNull());
        assertTrue(latecomerReview.path("questions").get(0).path("chosenOption").isNull());
    }

    @Test
    void aQuizPastItsWindowReadsAsClosedWithoutAnyoneClosingIt() {
        long quizId = startQuiz(leader);
        JsonNode questions = expect(call("POST", "/quizzes/" + quizId + "/start", member, null), 200).path("questions");
        database.update("update quizzes set created_at=now()-interval '1 hour', closes_at=now()-interval '1 minute' where id=?", quizId);
        database.update("update quiz_attempts set started_at=now()-interval '30 minutes' where quiz_id=?", quizId);

        assertEquals("CLOSED", expect(call("GET", "/quizzes/" + quizId, member, null), 200).path("status").asString());
        conflict(submit(quizId, member, answers(questions, QUESTIONS)), "QUIZ_CLOSED");
        expect(call("GET", "/quizzes/" + quizId + "/review", member, null), 200);
    }

    @Test
    void onlyHostsStartQuizzesAndOutsidersCannotSeeThem() {
        expect(createGroupQuiz(member, NOTES), 403);
        long quizId = startQuiz(leader);

        expect(call("GET", "/groups/" + groupId + "/quizzes", outsider, null), 403);
        expect(call("GET", "/quizzes/" + quizId, outsider, null), 404);
        expect(call("POST", "/quizzes/" + quizId + "/start", outsider, null), 404);
        expect(call("GET", "/quizzes/" + quizId + "/leaderboard", outsider, null), 404);
        expect(call("POST", "/quizzes/" + quizId + "/close", outsider, null), 404);
        expect(call("GET", "/quizzes/999999", member, null), 404);
        expect(call("GET", "/quizzes/" + quizId, null, null), 401);
    }

    @Test
    void theRoomsCoHostCanStartAQuiz() {
        Map<String, Object> settings = new java.util.HashMap<>();
        settings.put("focusMinutes", 25); settings.put("breakMinutes", 5); settings.put("participantLimit", 5);
        settings.put("hostId", leaderId); settings.put("coHostId", memberId); settings.put("expectedVersion", 0);
        expect(call("PUT", "/groups/" + groupId + "/room/settings", leader, settings), 200);

        long quizId = startQuiz(member);
        assertTrue(expect(call("GET", "/quizzes/" + quizId, member, null), 200).path("canHost").asBoolean());
    }

    @Test
    void anUnusableAiReplySavesNothing() {
        reply = null;
        Reply unavailable = createGroupQuiz(leader, NOTES);
        assertEquals(503, unavailable.status());
        assertEquals("AI_UNAVAILABLE", unavailable.payload().path("code").asString());

        reply = "{\"questions\": []}";
        assertEquals(503, createGroupQuiz(leader, NOTES).status());
        assertEquals(0, count("select count(*) from quizzes"));
    }

    @Test
    void refusesBadInputBeforeSpendingAnAiCall() {
        reply = null;
        expect(createGroupQuiz(leader, Map.of("notesText", "  ")), 400);
        expect(createGroupQuiz(leader, Map.of()), 400);
        expect(createGroupQuiz(leader, Map.of("notesText", "OOP", "focus", "x".repeat(201))), 400);
        expect(createGroupQuiz(leader, Map.of("notesText", "OOP", "sessionId", 999999)), 400);
        expect(createGroupQuiz(leader, NOTES, new Upload("week4.pdf", PDF)), 400);
        expect(createGroupQuiz(leader, Map.of(), new Upload("slides.pptx", PDF)), 415);
        assertTrue(AI_CALLS.isEmpty(), "Bad requests never reach Gemini");

        reply = TEN_QUESTIONS;
        long quizId = startQuiz(leader);
        JsonNode questions = expect(call("POST", "/quizzes/" + quizId + "/start", member, null), 200).path("questions");
        expect(submit(quizId, member, answers(questions, QUESTIONS).subList(0, 5)), 400);
        expect(call("POST", "/quizzes/" + quizId + "/attempts", member, Map.of()), 400);
        conflict(submit(quizId, latecomer, answers(questions, QUESTIONS)), "QUIZ_NOT_STARTED");
        expect(submit(quizId, member, answers(questions, QUESTIONS)), 201);
    }

    @Test
    void aGroupQuizCanBeForOneOfTheGroupsScheduledSessions() {
        reply = TEN_QUESTIONS;
        LocalDateTime start = LocalDateTime.now(ZoneOffset.UTC).plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Map<String, Object> details = Map.of("title", "Week 4 revision", "startLocal", start.toString(),
                "endLocal", start.plusHours(1).toString(), "timeZone", "Asia/Singapore", "occurrenceCount", 1);
        JsonNode session = expect(call("POST", "/groups/" + groupId + "/sessions", leader, details), 201);
        long sessionId = session.path("id").asLong();

        JsonNode created = expect(createGroupQuiz(leader, Map.of("notesText", "OOP", "sessionId", sessionId)), 201);
        assertEquals(sessionId, created.path("sessionId").asLong());
        assertEquals(sessionId, expect(call("GET", "/quizzes/" + created.path("id").asLong(), member, null), 200).path("sessionId").asLong());

        expect(call("POST", "/sessions/" + sessionId + "/cancel", leader, Map.of("expectedVersion", session.path("version").asLong())), 200);
        expect(createGroupQuiz(leader, Map.of("notesText", "OOP", "sessionId", sessionId)), 400);
    }

    @Test
    void aStudentPractisesAloneAndSeesTheAnswersAsSoonAsTheySubmit() {
        JsonNode created = expect(create("/quizzes", member, Map.of("courseId", course, "notesText", "Generics notes", "focus", "Generics")), 201);
        long quizId = created.path("id").asLong();
        assertEquals("PERSONAL", created.path("scope").asString());
        assertEquals("IS442", created.path("course").path("code").asString());
        assertTrue(created.path("groupId").isNull());
        assertTrue(created.path("canHost").asBoolean());

        assertEquals(1, expect(call("GET", "/quizzes", member, null), 200).size());
        assertEquals(0, expect(call("GET", "/quizzes", leader, null), 200).size(), "Personal quizzes are private");
        expect(call("GET", "/quizzes/" + quizId, leader, null), 404);
        expect(call("POST", "/quizzes/" + quizId + "/start", leader, null), 404);

        assertTrue(created.path("closesAt").isNull(), "A practice quiz has no time limit");
        // Long after a group quiz's window, a practice quiz is still waiting for its student.
        database.update("update quizzes set created_at=now()-interval '30 days' where id=?", quizId);
        assertEquals("OPEN", expect(call("GET", "/quizzes/" + quizId, member, null), 200).path("status").asString());

        JsonNode questions = expect(call("POST", "/quizzes/" + quizId + "/start", member, null), 200).path("questions");
        conflict(call("GET", "/quizzes/" + quizId + "/review", member, null), "QUIZ_NOT_CLOSED");
        JsonNode result = expect(submit(quizId, member, answers(questions, 7)), 201);
        assertEquals(7, result.path("score").asInt());
        assertEquals(1, result.path("rank").asInt());

        assertEquals("CLOSED", expect(call("GET", "/quizzes/" + quizId, member, null), 200).path("status").asString());
        JsonNode review = expect(call("GET", "/quizzes/" + quizId + "/review", member, null), 200);
        assertEquals(7, review.path("score").asInt());
        assertEquals("Because 0", review.path("questions").get(0).path("explanation").asString());
    }

    @Test
    void aPersonalQuizNeedsAKnownCourseAndSomeNotes() {
        expect(create("/quizzes", member, Map.of("courseId", 999999, "notesText", "Generics notes")), 400);
        expect(create("/quizzes", member, Map.of("notesText", "Generics notes")), 400);
        expect(create("/quizzes", member, Map.of("courseId", course, "notesText", " ")), 400);
        assertEquals(0, count("select count(*) from quizzes"));
    }

    @Test
    void uploadedSlidesGoToGeminiAsAttachmentsAndTitleTheQuiz() {
        JsonNode created = expect(createGroupQuiz(leader, Map.of(), new Upload("week4.pdf", PDF)), 201);

        assertEquals("week4.pdf", created.path("title").asString());
        assertEquals(1, AI_CALLS.size());
        assertEquals(1, AI_CALLS.get(0).size(), "The PDF is attached to the Gemini request");
        assertEquals("application/pdf", AI_CALLS.get(0).get(0).mediaType());
    }
}
