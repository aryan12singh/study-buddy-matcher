package com.studybuddy.integration;

import com.studybuddy.matchexplanation.ExplanationCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AI explanation cache and endpoint, over real PostgreSQL and HTTP. */
class MatchExplanationHttpTest extends PostgresHttpTest {
    private static final String HASH = "a".repeat(64);

    @Autowired
    private ExplanationCache cache;

    @Test
    void savingTheSameEvidenceTwiceKeepsOneRow() {
        assertTrue(cache.find(HASH).isEmpty());

        cache.save(HASH, "v1", "test-model", "First answer.");
        cache.save(HASH, "v1", "test-model", "Second answer.");

        assertEquals(Optional.of("First answer."), cache.find(HASH));
        assertEquals(1, count("select count(*) from match_explanations"));
    }

    @Test
    void explanationsFollowSearchRulesAndFallBackWhenAiIsOff() {
        long self = student("self@example.test", "Self", "81112222");
        long peer = student("peer@example.test", "Peer", "83334444");
        long inactive = student("inactive@example.test", "Inactive", "85556666");
        database.update("update users set active=false where id=?", inactive);
        account("admin@example.test", "ADMIN");
        String token = login("self@example.test");
        String adminToken = login("admin@example.test");
        String context = "/explanation?courseId=" + course;

        expect(call("GET", "/matches/" + peer + context, null, null), 401);
        expect(call("GET", "/matches/" + peer + context, adminToken, null), 403);
        expect(call("GET", "/matches/" + self + context, token, null), 404);
        expect(call("GET", "/matches/" + inactive + context, token, null), 404);
        expect(call("GET", "/matches/99999999" + context, token, null), 404);
        expect(call("GET", "/matches/" + peer + "/explanation", token, null), 400);

        Reply reply = call("GET", "/matches/" + peer + context, token, null);
        JsonNode explanation = expect(reply, 200);
        JsonNode card = expect(call("GET", "/matches?courseId=" + course, token, null), 200).path("matches").get(0);
        assertEquals(peer, explanation.path("candidateId").asLong());
        assertEquals("STANDARD", explanation.path("source").asString());
        assertEquals(card.path("explanation").asString(), explanation.path("text").asString());
        for (String forbidden : new String[] {"contactNumber", "email", "83334444", "Peer"}) {
            assertFalse(reply.body().contains(forbidden), reply.body());
        }
        assertEquals(0, count("select count(*) from match_explanations"));
    }
}
