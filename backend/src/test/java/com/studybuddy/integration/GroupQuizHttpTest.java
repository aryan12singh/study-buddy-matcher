package com.studybuddy.integration;

import com.studybuddy.ai.AiClient;
import com.studybuddy.ai.AiUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The real web, security, validation and database stack for group quizzes, with Gemini replaced by a fake. */
@Import(GroupQuizHttpTest.FakeAiConfiguration.class)
class GroupQuizHttpTest extends PostgresHttpTest {
    /** application.yml asks for this many questions per quiz. */
    private static final int QUESTIONS = 10;
    /** Question i's correct option is i % 4. */
    private static final String TEN_QUESTIONS = "{\"questions\": [" + IntStream.range(0, QUESTIONS)
            .mapToObj(i -> "{\"prompt\": \"Question " + i + "\", \"options\": [\"A\", \"B\", \"C\", \"D\"], \"correctOption\": "
                    + (i % 4) + ", \"explanation\": \"Because " + i + "\"}")
            .collect(Collectors.joining(", ")) + "]}";

    /** What the fake replies with; null makes it unavailable. Reset before each test. */
    private static volatile String reply;

    @TestConfiguration
    static class FakeAiConfiguration {
        @Bean
        @Primary
        AiClient fakeAi() {
            return (studentId, prompt, attachments) -> {
                if (reply == null) {
                    throw new AiUnavailableException();
                }
                return reply;
            };
        }
    }

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

    private long startQuiz(String token) {
        return expect(call("POST", "/groups/" + groupId + "/quizzes", token, Map.of("topics", "Polymorphism")), 201)
                .path("id").asLong();
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
        database.update("update group_quizzes set created_at=now()-interval '1 hour', closes_at=now()-interval '1 minute' where id=?", quizId);
        database.update("update quiz_attempts set started_at=now()-interval '30 minutes' where quiz_id=?", quizId);

        assertEquals("CLOSED", expect(call("GET", "/quizzes/" + quizId, member, null), 200).path("status").asString());
        conflict(submit(quizId, member, answers(questions, QUESTIONS)), "QUIZ_CLOSED");
        expect(call("GET", "/quizzes/" + quizId + "/review", member, null), 200);
    }

    @Test
    void onlyHostsStartQuizzesAndOutsidersCannotSeeThem() {
        expect(call("POST", "/groups/" + groupId + "/quizzes", member, Map.of("topics", "Polymorphism")), 403);
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
        Reply unavailable = call("POST", "/groups/" + groupId + "/quizzes", leader, Map.of("topics", "Polymorphism"));
        assertEquals(503, unavailable.status());
        assertEquals("AI_UNAVAILABLE", unavailable.payload().path("code").asString());

        reply = "{\"questions\": []}";
        assertEquals(503, call("POST", "/groups/" + groupId + "/quizzes", leader, Map.of("topics", "Polymorphism")).status());
        assertEquals(0, count("select count(*) from group_quizzes"));
    }

    @Test
    void refusesBadInputBeforeSpendingAnAiCall() {
        reply = null;
        expect(call("POST", "/groups/" + groupId + "/quizzes", leader, Map.of("topics", "  ")), 400);
        expect(call("POST", "/groups/" + groupId + "/quizzes", leader, Map.of("topics", "x".repeat(1001))), 400);
        expect(call("POST", "/groups/" + groupId + "/quizzes", leader, Map.of("topics", "OOP", "sessionId", 999999)), 400);

        reply = TEN_QUESTIONS;
        long quizId = startQuiz(leader);
        JsonNode questions = expect(call("POST", "/quizzes/" + quizId + "/start", member, null), 200).path("questions");
        expect(submit(quizId, member, answers(questions, QUESTIONS).subList(0, 5)), 400);
        expect(call("POST", "/quizzes/" + quizId + "/attempts", member, Map.of()), 400);
        conflict(submit(quizId, latecomer, answers(questions, QUESTIONS)), "QUIZ_NOT_STARTED");
        expect(submit(quizId, member, answers(questions, QUESTIONS)), 201);
    }
}
