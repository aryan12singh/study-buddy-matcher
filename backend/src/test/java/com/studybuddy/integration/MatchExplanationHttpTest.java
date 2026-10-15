package com.studybuddy.integration;

import com.studybuddy.matchexplanation.ExplanationCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
