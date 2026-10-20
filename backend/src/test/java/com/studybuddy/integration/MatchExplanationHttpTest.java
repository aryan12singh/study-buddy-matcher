package com.studybuddy.integration;

import com.studybuddy.matchexplanation.CriterionEvidence;
import com.studybuddy.matchexplanation.ExplanationCache;
import com.studybuddy.matchexplanation.ExplanationEvidence;
import com.studybuddy.matching.MatchQuality;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import java.util.Optional;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AI explanation cache and endpoint, over real PostgreSQL and HTTP. */
class MatchExplanationHttpTest extends PostgresHttpTest {
    /** Minimal anonymous evidence; the cache keys rows by its hash. */
    private static final ExplanationEvidence EVIDENCE = new ExplanationEvidence(MatchQuality.GOOD, 0.7,
            MatchingStrategyType.BALANCED, List.of(new CriterionEvidence(MatchingCriterion.COURSE, 1.0, 1.0, true, true)),
            2.0, null, null, null, List.of(), null, null, null, null);

    @Autowired
    private ExplanationCache cache;

    @Test
    void savingTheSameEvidenceTwiceKeepsOneRow() {
        assertTrue(cache.find(EVIDENCE).isEmpty());

        cache.save(EVIDENCE, "First answer.");
        cache.save(EVIDENCE, "Second answer.");

        assertEquals(Optional.of("First answer."), cache.find(EVIDENCE));
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
        JsonNode card = currentCard(token);
        String evaluationId = card.path("evaluationId").asString();
        String context = "/explanation?courseId=" + course + "&evaluationId=" + evaluationId;

        expect(call("GET", "/matches/" + peer + context, null, null), 401);
        expect(call("GET", "/matches/" + peer + context, adminToken, null), 403);
        expect(call("GET", "/matches/" + self + context, token, null), 404);
        expect(call("GET", "/matches/" + inactive + context, token, null), 404);
        expect(call("GET", "/matches/99999999" + context, token, null), 404);
        expect(call("GET", "/matches/" + peer + "/explanation", token, null), 400);
        expect(call("GET", "/matches/" + peer + "/explanation?courseId=" + course, token, null), 400);
        expect(call("GET", "/matches/" + peer + "/explanation?courseId=" + course + "&evaluationId=invalid", token, null), 400);

        Reply reply = call("GET", "/matches/" + peer + context, token, null);
        JsonNode explanation = expect(reply, 200);
        assertEquals(peer, explanation.path("candidateId").asLong());
        assertEquals(evaluationId, explanation.path("evaluationId").asString());
        assertEquals("STANDARD", explanation.path("source").asString());
        assertEquals(card.path("explanation").asString(), explanation.path("text").asString());
        for (String forbidden : new String[] {"contactNumber", "email", "83334444", "Peer"}) {
            assertFalse(reply.body().contains(forbidden), reply.body());
        }
        assertEquals(0, count("select count(*) from match_explanations"));
    }
    @ParameterizedTest
    @ValueSource(strings = {"configuration", "candidate-profile", "searcher-profile", "availability"})
    void changedScoringInputsRequireARefreshedCard(String changedInput) {
        long self = student("self@example.test", "Self", "81112222");
        long peer = student("peer@example.test", "Peer", "83334444");
        String token = login("self@example.test");
        JsonNode original = currentCard(token);
        String originalId = original.path("evaluationId").asString();

        switch (changedInput) {
            case "configuration" -> {
                account("admin@example.test", "ADMIN");
                Map<String, Double> modeOnly = Map.of("COURSE", 0.0, "AVAILABILITY", 0.0, "STUDY_MODE", 1.0,
                        "STUDY_GOAL", 0.0, "GROUP_SIZE", 0.0);
                expect(call("PUT", "/admin/matching-config", login("admin@example.test"),
                        Map.of("activeStrategy", "BALANCED", "enabledCriteria", List.of("STUDY_MODE"),
                                "minScore", 0, "expectedVersion", 0, "weights", Map.of("BALANCED", modeOnly,
                                        "AVAILABILITY_FIRST", modeOnly, "COURSE_FIRST", modeOnly))), 200);
            }
            case "candidate-profile" -> database.update("update students set preferred_study_mode='ONLINE' where id=?", peer);
            case "searcher-profile" -> database.update("insert into student_study_goals(student_id,study_goal) values (?,'EXAM_PREPARATION')", self);
            case "availability" -> database.update("update availability_slots set end_time='10:00' where student_id=?", peer);
            default -> throw new AssertionError(changedInput);
        }

        JsonNode conflict = expect(call("GET", explanationPath(peer, originalId), token, null), 409);
        assertEquals("STALE_MATCH_EVALUATION", conflict.path("code").asString());
        assertFalse(conflict.has("text"));
        JsonNode current = currentCard(token);
        String currentId = current.path("evaluationId").asString();
        assertNotEquals(originalId, currentId);
        JsonNode answer = expect(call("GET", explanationPath(peer, currentId), token, null), 200);
        assertEquals(currentId, answer.path("evaluationId").asString());
        assertEquals(current.path("explanation").asString(), answer.path("text").asString());
        assertEquals(0, count("select count(*) from match_explanations"));
    }

    @Test
    void requestRelationshipChangesDoNotInvalidateTheScoredCard() {
        student("self@example.test", "Self", "81112222");
        long peer = student("peer@example.test", "Peer", "83334444");
        String token = login("self@example.test");
        String originalId = currentCard(token).path("evaluationId").asString();

        send(peer, token);

        assertEquals(originalId, currentCard(token).path("evaluationId").asString());
        expect(call("GET", explanationPath(peer, originalId), token, null), 200);
    }

    private JsonNode currentCard(String token) {
        return expect(call("GET", "/matches?courseId=" + course, token, null), 200).path("matches").get(0);
    }

    private String explanationPath(long candidateId, String evaluationId) {
        return "/matches/" + candidateId + "/explanation?courseId=" + course + "&evaluationId=" + evaluationId;
    }
}
