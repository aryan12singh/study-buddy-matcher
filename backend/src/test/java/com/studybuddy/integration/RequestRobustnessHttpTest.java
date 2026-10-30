package com.studybuddy.integration;

import java.io.ByteArrayInputStream;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How the API answers requests that are malformed, oversized or aimed at the wrong place. */
@TestPropertySource(properties = {"app.http.max-json-body-size=4KB", "app.availability.max-weekly-slots=3",
        "app.study-group.max-capacity=6"})
class RequestRobustnessHttpTest extends PostgresHttpTest {

    private String signedIn() {
        student("alex@example.org", "Alex", "+6591234567");
        return login("alex@example.org");
    }

    @Test
    void anUnknownAddressIsNotFoundRatherThanAServerError() {
        JsonNode problem = expect(call("GET", "/no-such-endpoint", signedIn(), null), 404);
        assertEquals("NOT_FOUND", problem.path("code").asString());
    }

    @Test
    void anUnsupportedMethodIsRefusedWithTheMethodsThatAreAllowed() {
        Reply reply = call("DELETE", "/sessions/mine", signedIn(), null);
        assertEquals("METHOD_NOT_ALLOWED", expect(reply, 405).path("code").asString());
        assertTrue(reply.header("Allow").contains("GET"), reply.header("Allow"));
    }

    @Test
    void aBodyInAnUnsupportedFormatIsRefused() {
        Reply reply = raw("POST", "/match-requests", signedIn(), "text/plain", HttpRequest.BodyPublishers.ofString("hello"));
        assertEquals("UNSUPPORTED_MEDIA_TYPE", expect(reply, 415).path("code").asString());
    }

    @Test
    void aJsonBodyOverTheLimitIsRefusedBeforeItIsRead() {
        String token = signedIn();
        Map<String, Object> huge = Map.of("receiverId", 1, "message", "x".repeat(8 * 1024));
        assertEquals("REQUEST_TOO_LARGE", expect(call("POST", "/match-requests", token, huge), 413).path("code").asString());

        // Without a Content-Length the body is measured as it arrives.
        byte[] chunked = json.writeValueAsString(huge).getBytes(StandardCharsets.UTF_8);
        Reply streamed = raw("POST", "/match-requests", token, "application/json",
                HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(chunked)));
        assertEquals(413, streamed.status(), streamed.body());

        // A small chunked body still reaches the controller.
        byte[] small = json.writeValueAsString(Map.of("receiverId", 999999)).getBytes(StandardCharsets.UTF_8);
        Reply smallStreamed = raw("POST", "/match-requests", token, "application/json",
                HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(small)));
        assertEquals(404, smallStreamed.status(), smallStreamed.body());
    }

    @Test
    void aFractionIsRefusedForAWholeNumberField() {
        String token = signedIn();
        long other = student("sam@example.org", "Sam", "+6592345678");
        expect(call("POST", "/match-requests", token, Map.of("receiverId", other + 0.7)), 400);
        assertEquals(0, count("select count(*) from match_requests"));
    }

    @Test
    void textWithANulCharacterIsRefused() {
        String token = signedIn();
        long other = student("sam@example.org", "Sam", "+6592345678");
        expect(call("POST", "/match-requests", token, Map.of("receiverId", other, "message", "Hi\u0000there")), 400);
        assertEquals(0, count("select count(*) from match_requests"));
    }

    @Test
    void weeklyAvailabilityIsCappedForStudentsAndGroups() {
        String token = signedIn();
        List<Map<String, String>> slots = new ArrayList<>();
        for (String day : List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY")) {
            slots.add(Map.of("dayOfWeek", day, "startTime", "09:00", "endTime", "10:00"));
        }
        JsonNode refused = expect(call("PUT", "/profile/me/availability", token, Map.of("slots", slots)), 400);
        assertTrue(refused.path("fieldErrors").has("availability"), refused.toString());
        expect(call("PUT", "/profile/me/availability", token, Map.of("slots", slots.subList(0, 3))), 200);

        Map<String, Object> group = new java.util.HashMap<>(groupDetails("Revision", 4));
        group.put("availability", slots);
        assertTrue(expect(call("POST", "/groups", token, group), 400).path("fieldErrors").has("availability"));
    }

    @Test
    void aGroupCannotBeLargerThanTheConfiguredCapacity() {
        String token = signedIn();
        JsonNode refused = expect(call("POST", "/groups", token, groupDetails("Revision", 7)), 400);
        assertTrue(refused.path("fieldErrors").has("maxGroupSize"), refused.toString());
        expect(call("POST", "/groups", token, groupDetails("Revision", 6)), 201);
    }
}
