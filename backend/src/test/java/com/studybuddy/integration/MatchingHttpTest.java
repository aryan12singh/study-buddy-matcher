package com.studybuddy.integration;

import com.studybuddy.matching.config.MatchingConfigRepository;
import com.studybuddy.matching.config.MatchingConfigService;
import com.studybuddy.matching.config.MatchingConfigSnapshot;
import org.junit.jupiter.api.Test;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;

/** Uses real HTTP, JWT authorization, PostgreSQL persistence and matching scorers. */
class MatchingHttpTest extends PostgresHttpTest {

    // Test coordination retains real database configuration and scoring results.
    @MockitoSpyBean
    private MatchingConfigService config;
    @Autowired
    private MatchingConfigRepository weightRows;

    private Map<String, Object> settings(String strategy, Map<String, Double> balanced) {
        return Map.of("activeStrategy", strategy, "enabledCriteria", java.util.List.of("COURSE","AVAILABILITY","STUDY_MODE","STUDY_GOAL","GROUP_SIZE"),
                "minScore",0.0,"expectedVersion",count("select coalesce(max(config_version),0) from matching_strategy_settings"), "weights", Map.of(
                "BALANCED", balanced,
                "AVAILABILITY_FIRST", Map.of("AVAILABILITY", 0.0,"COURSE", 0.0, "STUDY_MODE", 0.0, "STUDY_GOAL", 1.0, "GROUP_SIZE", 0.0),
                "COURSE_FIRST", Map.of("COURSE", 0.0,"AVAILABILITY", 0.0, "STUDY_MODE", 0.0, "STUDY_GOAL", 1.0, "GROUP_SIZE", 0.0)));
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
        // Shifted rather than shortened: 90 of each student's 120 minutes overlap, an availability score of 0.75.
        database.update("update availability_slots set start_time='09:30', end_time='11:30' where student_id=?", peer);
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

    private Map<String, Object> singleCriterionSettings(String strategy, String criterion, double threshold) {
        Map<String, Double> values = new java.util.LinkedHashMap<>();
        for (String name : new String[]{"COURSE","AVAILABILITY","STUDY_MODE","STUDY_GOAL","GROUP_SIZE"}) {
            values.put(name, name.equals(criterion) ? 1.0 : 0.0);
        }
        return Map.of("activeStrategy", strategy, "weights", Map.of("BALANCED",values,"AVAILABILITY_FIRST",values,"COURSE_FIRST",values),
                "enabledCriteria",java.util.List.of(criterion),"minScore",threshold,
                "expectedVersion",count("select coalesce(max(config_version),0) from matching_strategy_settings"));
    }

    @Test
    void liveSortsFiltersMetadataAndConnectionExclusionFollowTheSameContract() {
        long self = student("self@example.test", "Self", "81112222");
        long strongest = student("strong@example.test", "Jamie", "83334444");
        long sameYear = student("year@example.test", "Alex", "85556666");
        database.update("update students set target_course_id=?,year_of_study=2,preferred_study_mode='ONLINE',preferred_group_size_min=2,preferred_group_size_max=2 where id=?", course,strongest);
        database.update("update students set preferred_study_mode='IN_PERSON',preferred_group_size_min=3,preferred_group_size_max=5 where id=?", sameYear);
        database.update("update availability_slots set end_time='10:00' where student_id=?", strongest);
        account("admin@example.test", "ADMIN");
        String studentToken = login("self@example.test");
        String adminToken = login("admin@example.test");
        var saved = expect(call("PUT", "/admin/matching-config", adminToken, singleCriterionSettings("BALANCED","COURSE",0)),200);
        String path = "/matches?courseId=" + course;
        var best = expect(call("GET",path,studentToken,null),200);
        assertEquals(1, best.path("configurationVersion").asLong());
        assertEquals(20, best.path("limit").asInt());
        assertEquals(2, best.path("totalMatches").asInt());
        assertEquals(strongest,best.path("matches").get(0).path("studentId").asLong());
        assertEquals(1.0,best.path("matches").get(0).path("totalScore").asDouble());
        assertEquals(course,best.path("matches").get(0).path("relevantCourse").path("id").asLong());
        assertTrue(best.path("matches").get(0).path("explanation").asString().startsWith("Strong match: same course."));
        assertFalse(best.path("matches").get(0).path("breakdown").get(1).path("enabled").asBoolean());
        for (String sort : new String[]{"MOST_SHARED_HOURS","SAME_YEAR_FIRST"}) {
            var results = expect(call("GET",path+"&sort="+sort,studentToken,null),200);
            assertEquals(sameYear,results.path("matches").get(0).path("studentId").asLong());
        }
        var filtered = expect(call("GET",path+"&studyGoal=CONCEPT_REVIEW&studyMode=ONLINE&groupSize=ONE_TO_ONE&minSharedHours=1&minQuality=STRONG",studentToken,null),200);
        assertEquals(1,filtered.path("matches").size());
        assertEquals(strongest,filtered.path("matches").get(0).path("studentId").asLong());
        assertEquals(0,expect(call("GET",path+"&minSharedHours=3",studentToken,null),200).path("matches").size());
        long request = send(strongest,studentToken);
        assertEquals("OUTGOING_PENDING",expect(call("GET",path,studentToken,null),200).path("matches").get(0).path("relationship").asString());
        expect(call("POST","/match-requests/"+request+"/accept",login("strong@example.test"),null),200);
        var connectedExcluded = expect(call("GET",path,studentToken,null),200);
        assertEquals(1,connectedExcluded.path("matches").size());
        assertEquals(sameYear,connectedExcluded.path("matches").get(0).path("studentId").asLong());
        assertEquals(saved.path("version").asLong(),connectedExcluded.path("configurationVersion").asLong());
        assertNotEquals(self,connectedExcluded.path("matches").get(0).path("studentId").asLong());
    }

    @Test
    void allSortTiesUseCaseInsensitiveNameThenIdAndResultsAreBounded() {
        student("self@example.test","Self","81112222");
        java.util.List<Long> expectedIds = new java.util.ArrayList<>();
        for (int index=0;index<25;index++) {
            expectedIds.add(student("peer"+index+"@example.test",index%2==0?"Jamie":"JAMIE","83334444"));
        }
        String token=login("self@example.test");
        for (String sort:new String[]{"BEST_MATCH","MOST_SHARED_HOURS","SAME_YEAR_FIRST"}) {
            var results=expect(call("GET","/matches?studyGoal=CONCEPT_REVIEW&sort="+sort+"&limit=3",token,null),200);
            assertEquals(25,results.path("totalMatches").asInt());
            assertEquals(3,results.path("matches").size());
            for(int index=0;index<3;index++) { assertEquals(expectedIds.get(index),results.path("matches").get(index).path("studentId").asLong()); }
        }
        assertEquals(20,expect(call("GET","/matches?courseId="+course,token,null),200).path("matches").size());
        assertEquals(25,expect(call("GET","/matches?courseId="+course+"&limit=50",token,null),200).path("matches").size());
        for(String query:new String[]{"", "studyMode=ONLINE", "courseId=-1", "courseId="+course+"&limit=0", "courseId="+course+"&limit=51", "courseId="+course+"&sort=UNKNOWN"}) {
            expect(call("GET","/matches?"+query,token,null),400);
        }
    }

    @Test
    void invalidAndStaleAdminUpdatesPreserveTheLastValidSettingsAndRestoreIsExplicit() {
        account("admin@example.test","ADMIN");
        String token=login("admin@example.test");
        var original=expect(call("GET","/admin/matching-config",token,null),200);
        assertTrue(original.path("usingDefaults").asBoolean());
        var saved=expect(call("PUT","/admin/matching-config",token,singleCriterionSettings("COURSE_FIRST","COURSE",0.8)),200);
        for(String field:new String[]{"activeStrategy","enabledCriteria","minScore","expectedVersion","weights"}) {
            Map<String,Object> invalid=new java.util.HashMap<>(singleCriterionSettings("BALANCED","COURSE",0));
            invalid.remove(field);
            expect(call("PUT","/admin/matching-config",token,invalid),400);
            assertEquals(saved,expect(call("GET","/admin/matching-config",token,null),200));
        }
        for(Object[] invalidField:new Object[][]{{"enabledCriteria",java.util.List.of()},{"minScore",-0.1},{"minScore",1.1},{"activeStrategy","UNKNOWN"}}) {
            Map<String,Object> invalid=new java.util.HashMap<>(singleCriterionSettings("BALANCED","COURSE",0));
            invalid.put((String)invalidField[0],invalidField[1]);
            expect(call("PUT","/admin/matching-config",token,invalid),400);
            assertEquals(saved,expect(call("GET","/admin/matching-config",token,null),200));
        }
        Map<String,Object> stale=new java.util.HashMap<>(singleCriterionSettings("BALANCED","COURSE",0));
        stale.put("expectedVersion",0L);
        assertEquals("STALE_MATCHING_CONFIG",expect(call("PUT","/admin/matching-config",token,stale),409).path("code").asString());
        expect(call("POST","/admin/matching-config/restore-defaults",token,Map.of("expectedVersion",0)),409);
        expect(call("POST","/admin/matching-config/restore-defaults",token,Map.of()),400);
        var restored=expect(call("POST","/admin/matching-config/restore-defaults",token,Map.of("expectedVersion",1)),200);
        assertEquals(2,restored.path("version").asLong());
        assertTrue(restored.path("usingDefaults").asBoolean());
        assertEquals(original.path("weights"),restored.path("weights"));
        assertEquals(0,count("select count(*) from matching_configs"));
        assertEquals(restored,expect(call("GET","/admin/matching-config",token,null),200));
    }

    @Test
    void thresholdAndDisabledPriorityApplyImmediatelyAndSearchFiltersStayIndependent() {
        long self=student("self@example.test","Self","81112222");
        long first=student("first@example.test","First","83334444");
        long second=student("second@example.test","Second","85556666");
        database.update("update students set target_course_id=? where id=?",course,first);
        // Shifted rather than shortened: 60 of each student's 120 minutes overlap, an availability score of 0.5.
        database.update("update availability_slots set start_time='10:00', end_time='12:00' where student_id=?",first);
        account("admin@example.test","ADMIN");
        String admin=login("admin@example.test"),token=login("self@example.test");
        String path="/matches?courseId="+course;
        expect(call("PUT","/admin/matching-config",admin,singleCriterionSettings("COURSE_FIRST","AVAILABILITY",0.75)),200);
        var result=expect(call("GET",path,token,null),200);
        assertEquals(1,result.path("matches").size());
        assertEquals(second,result.path("matches").get(0).path("studentId").asLong());
        expect(call("PUT","/admin/matching-config",admin,singleCriterionSettings("AVAILABILITY_FIRST","COURSE",0)),200);
        result=expect(call("GET",path,token,null),200);
        assertEquals(first,result.path("matches").get(0).path("studentId").asLong());
        result=expect(call("GET",path+"&minSharedHours=2",token,null),200);
        assertEquals(1,result.path("matches").size());
        assertEquals(second,result.path("matches").get(0).path("studentId").asLong());
        assertNotEquals(self,result.path("matches").get(0).path("studentId").asLong());
    }

    @Test
    void differentAdminsRacingOnFirstSaveCannotBothReplaceVersionZero() throws Exception {
        account("admin1@example.test","ADMIN");account("admin2@example.test","ADMIN");
        String first=login("admin1@example.test"),second=login("admin2@example.test");
        var request=singleCriterionSettings("BALANCED","COURSE",0.5);
        statuses(race(() -> call("PUT","/admin/matching-config",first,request),
                () -> call("PUT","/admin/matching-config",second,request)),200,409);
        assertEquals(1,count("select count(*) from matching_strategy_settings"));
        assertEquals(1,count("select config_version from matching_strategy_settings"));
        assertEquals(15,count("select count(*) from matching_configs"));
    }

    @Test
    void differentAdminsRacingSaveAndRestoreCannotLoseANewerConfiguration() throws Exception {
        account("admin1@example.test","ADMIN");account("admin2@example.test","ADMIN");
        String first=login("admin1@example.test"),second=login("admin2@example.test");
        expect(call("PUT","/admin/matching-config",first,singleCriterionSettings("BALANCED","COURSE",0.5)),200);
        var next=singleCriterionSettings("COURSE_FIRST","COURSE",0.8);
        statuses(race(() -> call("PUT","/admin/matching-config",first,next),
                () -> call("POST","/admin/matching-config/restore-defaults",second,Map.of("expectedVersion",1))),200,409);
        var active=expect(call("GET","/admin/matching-config",first,null),200);
        assertEquals(2,active.path("version").asLong());
        assertEquals(active.path("usingDefaults").asBoolean()?0:15,count("select count(*) from matching_configs"));
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

        CountDownLatch settingsRead = new CountDownLatch(1);
        CountDownLatch saveFinished = new CountDownLatch(1);
        Advised repository = (Advised) weightRows;
        AtomicBoolean firstRead = new AtomicBoolean(true);
        MethodInterceptor pauseBeforeWeights = invocation -> {
            if (invocation.getMethod().getName().equals("findAll") && invocation.getArguments().length == 0
                    && firstRead.getAndSet(false)) {
                settingsRead.countDown();
                assertTrue(saveFinished.await(10, TimeUnit.SECONDS), "Admin save did not finish");
            }
            return invocation.proceed();
        };
        if (saveInitialSettings) {
            // Pause between the real settings query and the real weights query.
            repository.addAdvice(0, pauseBeforeWeights);
        } else {
            // With no override, defaults need no second database read.
            doAnswer(invocation -> {
                MatchingConfigSnapshot settings = (MatchingConfigSnapshot) invocation.callRealMethod();
                settingsRead.countDown();
                assertTrue(saveFinished.await(10, TimeUnit.SECONDS), "Admin save did not finish");
                return settings;
            }).when(config).snapshot();
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var searching = executor.submit(() -> call("GET", path, studentToken, null));
            try {
                assertTrue(settingsRead.await(10, TimeUnit.SECONDS), "Search did not read its settings");
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
        } finally {
            if (saveInitialSettings) repository.removeAdvice(pauseBeforeWeights);
        }
    }
}
