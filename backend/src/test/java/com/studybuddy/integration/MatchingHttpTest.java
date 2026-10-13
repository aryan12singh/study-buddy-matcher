package com.studybuddy.integration;

import com.studybuddy.matching.config.MatchingConfigService;
import com.studybuddy.matching.scoring.MatchingWeights;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;

/** Uses real HTTP, JWT authorization, PostgreSQL persistence and matching scorers. */
class MatchingHttpTest extends PostgresHttpTest {

    // The spy pauses only after the real read; no configuration or scoring results are replaced.
    @MockitoSpyBean
    private MatchingConfigService config;

    private Map<String, Object> settings(String strategy, Map<String, Double> balanced) {
        return Map.of("activeStrategy", strategy, "weights", Map.of(
                "BALANCED", balanced,
                "AVAILABILITY_FIRST", Map.of("COURSE", 0.0, "STUDY_MODE", 0.0, "STUDY_GOAL", 1.0, "GROUP_SIZE", 0.0),
                "COURSE_FIRST", Map.of("AVAILABILITY", 0.0, "STUDY_MODE", 0.0, "STUDY_GOAL", 1.0, "GROUP_SIZE", 0.0)));
    }

    private Map<String, Double> balanced(double courseWeight, double availabilityWeight, double goalWeight) {
        return Map.of("COURSE", courseWeight, "AVAILABILITY", availabilityWeight,
                "STUDY_MODE", 0.0, "STUDY_GOAL", goalWeight, "GROUP_SIZE", 0.0);
    }

    @Test
    void courseAndGoalSearchesRequireStudentAuthorizationAndWithholdPrivateFields() {
        long self = student("self@example.test", "Self", "81112222");
        long peer = student("peer@example.test", "Peer", "83334444");
        long inactive = student("inactive@example.test", "Inactive", "85556666");
        database.update("update users set active=false where id=?", inactive);
        account("admin@example.test", "ADMIN");
        String studentToken = login("self@example.test");
        String adminToken = login("admin@example.test");

        expect(call("GET", "/matches?courseId=" + course, null, null), 401);
        expect(call("GET", "/matches?courseId=" + course, adminToken, null), 403);
        expect(call("GET", "/admin/matching-config", studentToken, null), 403);
        expect(call("PUT", "/admin/matching-config", studentToken,
                settings("BALANCED", balanced(1.0, 0.0, 0.0))), 403);
        expect(call("GET", "/matches?minSharedHours=0", studentToken, null), 400);
        expect(call("GET", "/matches?courseId=99999999", studentToken, null), 404);
        for (String query : new String[] {"courseId=" + course, "studyGoal=CONCEPT_REVIEW"}) {
            Reply reply = call("GET", "/matches?" + query, studentToken, null);
            var matches = expect(reply, 200).path("matches");
            assertEquals(1, matches.size());
            assertEquals(peer, matches.get(0).path("studentId").asLong());
            assertNotEquals(self, matches.get(0).path("studentId").asLong());
            for (String forbidden : new String[] {"contactNumber", "passwordHash", "email", "83334444"}) {
                assertFalse(reply.body().contains(forbidden), reply.body());
            }
        }
        expect(call("GET", "/admin/matching-config", adminToken, null), 200);
    }

    @Test
    void fractionalWeightsSurviveSaveReadAndDetermineSearchQuality() {
        long self = student("self@example.test", "Self", "81112222");
        long peer = student("peer@example.test", "Peer", "83334444");
        database.update("update students set target_course_id=? where id=?", course, self);
        database.update("delete from student_courses where student_id=?", peer);
        database.update("update availability_slots set end_time='10:30' where student_id=?", peer);
        account("admin@example.test", "ADMIN");
        String adminToken = login("admin@example.test");
        String studentToken = login("self@example.test");

        var saved = expect(call("PUT", "/admin/matching-config", adminToken,
                settings("BALANCED", balanced(0.004, 0.996, 0.0))), 200);
        var reloaded = expect(call("GET", "/admin/matching-config", adminToken, null), 200);

        assertEquals(0.004, saved.path("weights").path("BALANCED").path("COURSE").asDouble());
        assertEquals(0.996, saved.path("weights").path("BALANCED").path("AVAILABILITY").asDouble());
        assertEquals(saved, reloaded);
        assertEquals(0.004, database.queryForObject(
                "select weight from matching_configs where strategy='BALANCED' and criterion='COURSE'", Double.class));

        // Course=0 and availability=.75 yield .747, below STRONG=.75. Rounding to 0/1 changes the label.
        var precise = expect(call("GET", "/matches?studyGoal=CONCEPT_REVIEW", studentToken, null), 200);
        assertEquals("BALANCED", precise.path("strategy").asString());
        assertEquals(1, precise.path("matches").size());
        assertEquals(peer, precise.path("matches").get(0).path("studentId").asLong());
        assertEquals("GOOD", precise.path("matches").get(0).path("quality").asString());

        expect(call("PUT", "/admin/matching-config", adminToken,
                settings("BALANCED", balanced(0.0, 1.0, 0.0))), 200);
        var wholePercentage = expect(call("GET", "/matches?studyGoal=CONCEPT_REVIEW", studentToken, null), 200);
        assertEquals("STRONG", wholePercentage.path("matches").get(0).path("quality").asString());
    }

    @Test
    void thirdsSurviveAnHttpSaveAndReadWithoutBeingRoundedToHundredths() {
        account("admin@example.test", "ADMIN");
        String adminToken = login("admin@example.test");
        double third = 1.0 / 3;
        var saved = expect(call("PUT", "/admin/matching-config", adminToken,
                settings("BALANCED", balanced(third, third, third))), 200);
        var reloaded = expect(call("GET", "/admin/matching-config", adminToken, null), 200);

        assertEquals(third, saved.path("weights").path("BALANCED").path("COURSE").asDouble());
        assertEquals(third, saved.path("weights").path("BALANCED").path("AVAILABILITY").asDouble());
        assertEquals(third, saved.path("weights").path("BALANCED").path("STUDY_GOAL").asDouble());
        assertEquals(saved, reloaded);
    }

    @Test
    void firstAdminSaveCannotMixANewStrategyWithDefaultWeightsDuringSearch() throws Exception {
        assertConcurrentSearchUsesOneConfiguration(false);
    }

    @Test
    void laterAdminSaveCannotMixANewStrategyWithPreviouslySavedWeightsDuringSearch() throws Exception {
        assertConcurrentSearchUsesOneConfiguration(true);
    }

    private void assertConcurrentSearchUsesOneConfiguration(boolean saveInitialSettings) throws Exception {
        student("self@example.test", "Self", "81112222");
        student("peer@example.test", "Peer", "83334444");
        account("admin@example.test", "ADMIN");
        String studentToken = login("self@example.test");
        String adminToken = login("admin@example.test");
        if (saveInitialSettings) {
            expect(call("PUT", "/admin/matching-config", adminToken,
                    settings("BALANCED", balanced(1.0, 0.0, 0.0))), 200);
        }
        assertEquals(saveInitialSettings ? 15 : 0, count("select count(*) from matching_configs"));
        assertEquals(saveInitialSettings ? 1 : 0, count("select count(*) from matching_strategy_settings"));
        String path = "/matches?courseId=" + course;
        var before = expect(call("GET", path, studentToken, null), 200);
        assertEquals("BALANCED", before.path("strategy").asString());
        assertEquals("GOOD", before.path("matches").get(0).path("quality").asString());

        CountDownLatch weightsRead = new CountDownLatch(1);
        CountDownLatch saveFinished = new CountDownLatch(1);
        doAnswer(invocation -> {
            MatchingWeights weights = (MatchingWeights) invocation.callRealMethod();
            weightsRead.countDown();
            assertTrue(saveFinished.await(10, TimeUnit.SECONDS), "Admin save did not finish");
            return weights;
        }).when(config).currentWeights();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var searching = executor.submit(() -> call("GET", path, studentToken, null));
            try {
                assertTrue(weightsRead.await(10, TimeUnit.SECONDS), "Search did not read its weights");
                expect(call("PUT", "/admin/matching-config", adminToken,
                        settings("AVAILABILITY_FIRST", balanced(0.0, 0.0, 1.0))), 200);
            } finally {
                saveFinished.countDown();
            }
            var concurrent = expect(searching.get(10, TimeUnit.SECONDS), 200);
            var after = expect(call("GET", path, studentToken, null), 200);
            assertEquals("AVAILABILITY_FIRST", after.path("strategy").asString());
            assertEquals("STRONG", after.path("matches").get(0).path("quality").asString());
            assertTrue(concurrent.equals(before) || concurrent.equals(after),
                    "A concurrent search must use a complete old or new configuration: " + concurrent);
        }
    }
}
