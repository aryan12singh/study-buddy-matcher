package com.studybuddy.integration;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/** Request links must follow the recipient's participation, including lifecycle withdrawal. */
class NotificationDirectionTest extends PostgresHttpTest {

    @Test
    void senderDeactivationLeavesAnIncomingWithdrawalAndDeclinedHistoryForTheReceiver() {
        long sender = student("sender@example.test", "Sender", "Sender contact");
        long receiver = student("receiver@example.test", "Receiver", "Receiver contact");
        account("admin@example.test", "ADMIN");
        String senderToken = login("sender@example.test");
        String receiverToken = login("receiver@example.test");
        String adminToken = login("admin@example.test");
        long request = send(receiver, senderToken);

        expect(call("POST", "/admin/users/" + sender + "/deactivate", adminToken, null), 200);

        JsonNode events = expect(call("GET", "/notifications?filter=REQUESTS", receiverToken, null), 200);
        JsonNode withdrawal = requestEvent(events, request, "MATCH_REQUEST_DECLINED");
        assertEquals("INCOMING", withdrawal.path("requestDirection").asString());
        assertEquals("INCOMING", requestEvent(events, request, "MATCH_REQUEST_RECEIVED")
                .path("requestDirection").asString());
        JsonNode history = expect(call("GET", "/match-requests/incoming", receiverToken, null), 200);
        assertEquals(1, history.size());
        assertEquals(request, history.get(0).path("id").asLong());
        assertEquals(sender, history.get(0).path("senderId").asLong());
        assertEquals(receiver, history.get(0).path("receiverId").asLong());
        assertEquals("DECLINED", history.get(0).path("status").asString());
        assertEquals(0, expect(call("GET", "/match-requests/outgoing", receiverToken, null), 200).size());

        JsonNode read = expect(call("POST", "/notifications/" + withdrawal.path("id").asLong() + "/read",
                receiverToken, null), 200);
        assertEquals("INCOMING", read.path("requestDirection").asString());
    }

    @Test
    void acceptedRequestNotificationLinksToTheSendersOutgoingHistory() {
        assertDecisionDirection("accept", "MATCH_REQUEST_ACCEPTED", "ACCEPTED");
    }

    @Test
    void declinedRequestNotificationLinksToTheSendersOutgoingHistory() {
        assertDecisionDirection("decline", "MATCH_REQUEST_DECLINED", "DECLINED");
    }

    private void assertDecisionDirection(String action, String eventType, String status) {
        student("sender@example.test", "Sender", "Sender contact");
        long receiver = student("receiver@example.test", "Receiver", "Receiver contact");
        String senderToken = login("sender@example.test");
        String receiverToken = login("receiver@example.test");
        long request = send(receiver, senderToken);

        expect(call("POST", "/match-requests/" + request + "/" + action, receiverToken, null), 200);

        JsonNode events = expect(call("GET", "/notifications?filter=REQUESTS", senderToken, null), 200);
        JsonNode decision = requestEvent(events, request, eventType);
        assertEquals("OUTGOING", decision.path("requestDirection").asString());
        JsonNode history = expect(call("GET", "/match-requests/outgoing", senderToken, null), 200);
        assertEquals(1, history.size());
        assertEquals(request, history.get(0).path("id").asLong());
        assertEquals(status, history.get(0).path("status").asString());
        assertEquals(0, expect(call("GET", "/match-requests/incoming", senderToken, null), 200).size());
    }

    private static JsonNode requestEvent(JsonNode events, long requestId, String type) {
        for (JsonNode event : events) {
            if (event.path("resourceType").asString().equals("MATCH_REQUEST")
                    && event.path("resourceId").asLong() == requestId
                    && event.path("type").asString().equals(type)) {
                return event;
            }
        }
        return fail("Missing " + type + " event for request " + requestId);
    }
}
