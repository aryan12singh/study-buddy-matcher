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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The real web, security, matching and session stack behind the assistant, with Gemini replaced by a fake. */
@Import(AssistantHttpTest.FakeAiConfiguration.class)
class AssistantHttpTest extends PostgresHttpTest {
    /** What the fake replies with; null makes it unavailable. Reset before each test. */
    private static volatile String reply;
    private static final List<String> PROMPTS = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class FakeAiConfiguration {
        @Bean
        @Primary
        AiClient fakeAi() {
            return (studentId, prompt, attachments) -> {
                PROMPTS.add(prompt);
                if (reply == null) {
                    throw new AiUnavailableException();
                }
                return reply;
            };
        }
    }

    private static String understood(String intent, String details) {
        return "{\"intent\": \"" + intent + "\"" + (details.isEmpty() ? "" : ", " + details) + "}";
    }

    private Reply ask(String token, String message) {
        return call("POST", "/assistant/messages", token, Map.of("message", message));
    }

    @BeforeEach
    void resetFakeAi() {
        reply = understood("UNKNOWN", "");
        PROMPTS.clear();
    }

    @Test
    void findsRealBuddiesWithoutAnyContactDetails() {
        student("priya@example.org", "Priya", "+6591234567");
        student("marcus@example.org", "Marcus", "+6598765432");
        String priya = login("priya@example.org");
        reply = understood("FIND_BUDDIES", "\"courseCode\": \"is 442\"");

        Reply answer = ask(priya, "find me an IS442 buddy");
        JsonNode body = expect(answer, 200);

        assertEquals("FIND_BUDDIES", body.path("intent").asString());
        assertEquals(1, body.path("matches").size());
        assertEquals("Marcus", body.path("matches").get(0).path("name").asString());
        assertEquals("IS442 Object Oriented Programming", body.path("filters").get(0).asString());
        assertTrue(body.path("text").asString().startsWith("I found 1 match for IS442."));
        assertFalse(answer.body().contains("+6598765432"), "No contact number reaches the assistant's reply");
        assertFalse(answer.body().contains("@example.org"), "No email reaches the assistant's reply");
        assertTrue(PROMPTS.get(0).contains("find me an IS442 buddy"));
        assertFalse(PROMPTS.get(0).contains("Marcus"), "No other student's data goes to the AI");
    }

    @Test
    void findsWhenTheStudentsOwnGroupIsFree() {
        student("lead@example.org", "Lena", "+6591110001");
        student("member@example.org", "Marcus", "+6591110002");
        String leader = login("lead@example.org");
        String member = login("member@example.org");
        long groupId = group(leader, 5);
        expect(approve(groupId, apply(groupId, member), leader), 200);
        reply = understood("WHEN_TO_MEET", "\"durationMinutes\": 60");

        JsonNode body = expect(ask(member, "when can my group meet for an hour?"), 200);

        assertEquals("WHEN_TO_MEET", body.path("intent").asString());
        assertEquals(1, body.path("slots").size(), "Both members are free on Monday 09:00-11:00");
        assertEquals("MONDAY", body.path("slots").get(0).path("dayOfWeek").asString());
        assertEquals("/groups/" + groupId + "/sessions/new", body.path("action").path("link").asString());
    }

    @Test
    void handsOverToQuizBuddyAndStudyPlansAndExplainsWhatItCanDo() {
        student("priya@example.org", "Priya", "+6591234567");
        String priya = login("priya@example.org");

        reply = understood("QUIZ_ME", "\"topic\": \"polymorphism\"");
        JsonNode quiz = expect(ask(priya, "quiz me on polymorphism"), 200);
        assertEquals("OPEN_QUIZ_BUDDY", quiz.path("action").path("kind").asString());

        reply = understood("PLAN_STUDY", "\"courseCode\": \"IS442\", \"examDate\": \"2026-11-20\", \"hoursPerWeek\": 6");
        JsonNode plan = expect(ask(priya, "plan my IS442 revision, exam 20 Nov, 6 hours a week"), 200);
        assertEquals("/study-plan?courseId=" + course + "&examDate=2026-11-20&hoursPerWeek=6", plan.path("action").path("link").asString());

        reply = understood("UNKNOWN", "");
        assertTrue(expect(ask(priya, "hello"), 200).path("text").asString().startsWith("I can tell you what's waiting for you"));
    }

    @Test
    void refusesBadMessagesBeforeAskingTheAiAndSaysWhenItIsUnavailable() {
        student("priya@example.org", "Priya", "+6591234567");
        String priya = login("priya@example.org");

        expect(ask(priya, "   "), 400);
        expect(ask(priya, "x".repeat(501)), 400);
        expect(ask(null, "hello"), 401);
        assertTrue(PROMPTS.isEmpty());

        reply = "Sure, here's a buddy!";
        assertEquals("AI_UNAVAILABLE", expect(ask(priya, "find me a buddy"), 503).path("code").asString());
        reply = null;
        assertEquals("AI_UNAVAILABLE", expect(ask(priya, "find me a buddy"), 503).path("code").asString());
    }

    @Test
    void listsRealPendingRequestsAndPointsToPages() {
        long priyaId = student("priya@example.org", "Priya", "+6591234567");
        student("marcus@example.org", "Marcus", "+6598765432");
        String priya = login("priya@example.org");
        send(priyaId, login("marcus@example.org"));

        reply = understood("MY_TO_DO", "");
        JsonNode todo = expect(ask(priya, "do I have any pending buddy requests?"), 200);
        assertEquals("1 buddy request from Marcus waiting for your reply", todo.path("todos").get(0).path("text").asString());
        assertEquals("/connections?view=incoming", todo.path("todos").get(0).path("link").asString());

        reply = understood("NAVIGATE", "\"page\": \"EDIT_PROFILE\"");
        JsonNode go = expect(ask(priya, "where do I change my availability?"), 200);
        assertEquals("/students/" + priyaId + "/edit", go.path("action").path("link").asString());
    }
}
