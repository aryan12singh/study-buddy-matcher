package com.studybuddy.integration;

import com.studybuddy.student.AvailabilitySlotRepository;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Authenticated whole-profile writes must keep each account's submitted collections together. */
class ProfileConcurrencyTest extends PostgresHttpTest {
    @Autowired
    private AvailabilitySlotRepository availability;

    @Test
    void concurrentWholeWeekReplacementsLeaveTheLaterWeekAndDoNotBlockAnotherAccount() throws Exception {
        long owner = student("availability@example.test", "Availability", "Synthetic contact");
        student("other@example.test", "Other", "Other synthetic contact");
        String token = login("availability@example.test");
        String otherToken = login("other@example.test");
        database.update("delete from availability_slots where student_id = ?", owner);

        var replies = replaceInOrder("flush", "/profile/me/availability", token,
                slots("09:00", "11:00"), slots("10:00", "12:00"),
                () -> expect(call("PUT", "/profile/me/availability", otherToken, slots("14:00", "16:00")), 200));

        expect(replies.first(), 200);
        var laterWeek = expect(replies.second(), 200);
        var saved = expect(call("GET", "/profile/me", token, null), 200).path("availability");
        assertEquals(laterWeek, saved,
                "Whole-week replacement must retain the later submitted week rather than combine both weeks");
        assertTrue(replies.waited(), "The later replacement must wait for the same account's first transaction");
        assertEquals(1, count("select count(*) from availability_slots where student_id = ?", owner));
    }

    @Test
    void concurrentProfileReplacementsLeaveTheLaterCoursesGoalsAndDetailsTogether() throws Exception {
        long owner = student("profile@example.test", "Original", "Synthetic contact");
        String token = login("profile@example.test");
        long firstCourse = database.queryForObject(
                "insert into courses(code, name) values ('IS212', 'Software Project Management') returning id", Long.class);
        long laterCourse = database.queryForObject(
                "insert into courses(code, name) values ('IS210', 'Business Process Analysis') returning id", Long.class);

        var replies = replaceInOrder("findByStudentId", "/profile/me", token,
                profile("First", firstCourse, "EXAM_PREPARATION"),
                profile("Later", laterCourse, "PROBLEM_SOLVING"), () -> { });

        expect(replies.first(), 200);
        var laterProfile = expect(replies.second(), 200);
        var saved = expect(call("GET", "/profile/me", token, null), 200);
        assertEquals(laterProfile, saved,
                "The later replacement must retain its profile fields, courses and study goals together");
        assertTrue(replies.waited(), "The later profile update must wait before reading the first update's collections");
        assertEquals(List.of(laterCourse), database.queryForList(
                "select course_id from student_courses where student_id = ?", Long.class, owner));
        assertEquals(List.of("PROBLEM_SOLVING"), database.queryForList(
                "select study_goal from student_study_goals where student_id = ?", String.class, owner));
    }

    private OrderedReplies replaceInOrder(String heldMethod, String path, String token, Object firstBody,
            Object laterBody, Runnable whileFirstIsHeld) throws Exception {
        try (var held = new HeldFirstWrite(heldMethod); var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Reply> first = executor.submit(() -> call("PUT", path, token, firstBody));
            try {
                int firstPid = held.backend.get(8, TimeUnit.SECONDS);
                whileFirstIsHeld.run();
                Future<Reply> later = executor.submit(() -> call("PUT", path, token, laterBody));
                boolean waited = awaitAccountRowLock(firstPid, later);
                held.release.countDown();
                return new OrderedReplies(first.get(8, TimeUnit.SECONDS), later.get(8, TimeUnit.SECONDS), waited);
            } finally {
                // Release before the executor closes, including assertion and HTTP failures.
                held.release.countDown();
            }
        }
    }

    private boolean awaitAccountRowLock(int firstPid, Future<Reply> later) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
        while (System.nanoTime() < deadline) {
            if (count("""
                    select count(*) from pg_stat_activity
                    where datname = current_database()
                      and wait_event_type = 'Lock'
                      and ? = any(pg_blocking_pids(pid))
                      and lower(query) like '%users%'
                      and lower(query) like '%for %update%'
                    """, firstPid) > 0) {
                return true;
            }
            if (later.isDone()) {
                return false;
            }
            Thread.sleep(25);
        }
        fail("PostgreSQL never reported the later profile write waiting for the held account row");
        return false;
    }

    private Map<String, Object> slots(String start, String end) {
        return Map.of("slots", List.of(Map.of("dayOfWeek", "MONDAY", "startTime", start, "endTime", end)));
    }

    private Map<String, Object> profile(String name, long courseId, String goal) {
        return Map.of("name", name, "school", "SCIS", "programme", "Information Systems", "yearOfStudy", 3,
                "contactNumber", name + " contact", "courseIds", List.of(courseId), "preferredStudyMode", "ONLINE",
                "groupSizePreference", "EITHER", "studyGoals", List.of(goal));
    }

    private record OrderedReplies(Reply first, Reply second, boolean waited) { }

    /** Holds only the first writer; the second is free to reach or wait for its account row lock. */
    private class HeldFirstWrite implements AutoCloseable {
        private final CompletableFuture<Integer> backend = new CompletableFuture<>();
        private final CountDownLatch release = new CountDownLatch(1);
        private final Advised repository = (Advised) availability;
        private final MethodInterceptor gate;

        private HeldFirstWrite(String method) {
            AtomicBoolean firstCall = new AtomicBoolean(true);
            gate = invocation -> {
                if (!invocation.getMethod().getName().equals(method) || !firstCall.compareAndSet(true, false)) {
                    return invocation.proceed();
                }
                // Availability pauses after deletion; profile pauses before its response query can flush changes.
                boolean afterFlush = method.equals("flush");
                Object result = afterFlush ? invocation.proceed() : null;
                backend.complete(database.queryForObject("select pg_backend_pid()", Integer.class));
                if (!release.await(15, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting to release the first profile write");
                }
                return afterFlush ? result : invocation.proceed();
            };
            repository.addAdvice(0, gate);
        }

        @Override
        public void close() {
            release.countDown();
            repository.removeAdvice(gate);
        }
    }
}
