package com.studybuddy.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Runs the real web/security/JPA stack against migrated PostgreSQL, without mocks.
 * The fixture reset deliberately refuses every database except the disposable test one.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class PostgresHttpTest {
    @Autowired protected JdbcTemplate database;
    @Autowired protected PasswordEncoder passwords;
    @Autowired protected Environment environment;
    @LocalServerPort private int port;

    protected final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String fixturePassword = UUID.randomUUID().toString();
    private String passwordHash;
    protected long course;

    @BeforeEach
    void resetDisposableDatabase() {
        assertEquals("studybuddy_test", database.queryForObject("select current_database()", String.class),
                "Fixture deletion must never run against the shared project database");
        database.execute("alter table notifications drop constraint if exists test_notification_failure");
        database.execute("truncate users, courses, matching_configs, matching_strategy_settings restart identity cascade");
        if (passwordHash == null) {
            passwordHash = passwords.encode(fixturePassword);
        }
        course = database.queryForObject("insert into courses(code,name) values ('IS442','Object Oriented Programming') returning id", Long.class);
    }

    protected long student(String email, String name, String contact) {
        long id = account(email, "STUDENT");
        database.update("insert into students(id,name,school,programme,year_of_study,contact_number) values (?,?,?,?,?,?)",
                id, name, "SCIS", "Information Systems", 3, contact);
        database.update("insert into student_courses(student_id,course_id) values (?,?)", id, course);
        database.update("insert into student_study_goals(student_id,study_goal) values (?,?)", id, "CONCEPT_REVIEW");
        database.update("insert into availability_slots(student_id,day_of_week,start_time,end_time) values (?,'MONDAY','09:00','11:00')", id);
        return id;
    }

    protected long account(String email, String role) {
        return database.queryForObject("insert into users(email,password_hash,role,active,created_at) values (?,?,?,true,now()) returning id",
                Long.class, email, passwordHash, role);
    }

    /** A login attempt with the fixture password or a wrong one, returned without asserting success. */
    protected Reply attemptLogin(String email, boolean correctPassword) {
        return call("POST", "/auth/login", null, Map.of("email", email, "password", correctPassword ? fixturePassword : "not-the-password"));
    }

    protected String login(String email) {
        return expect(call("POST", "/auth/login", null,
                Map.of("email", email, "password", fixturePassword)), 200).path("token").asString();
    }

    protected Reply call(String method, String path, String token, Object body) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api" + path))
                    .timeout(Duration.ofSeconds(20)).header("Accept", "application/json");
            if (token != null) {
                request.header("Authorization", "Bearer " + token);
            }
            if (body != null) {
                request.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            } else {
                request.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode payload = response.body().isBlank() ? json.nullNode() : json.readTree(response.body());
            return new Reply(response.statusCode(), payload, response.body(),
                    response.headers().firstValue("content-type").orElse(""));
        } catch (Exception error) {
            throw new AssertionError("HTTP request failed: " + method + " " + path, error);
        }
    }

    protected JsonNode expect(Reply response, int status) {
        assertEquals(status, response.status(), response.body());
        return response.payload();
    }

    protected long send(long receiver, String senderToken) {
        return expect(call("POST", "/match-requests", senderToken,
                Map.of("receiverId", receiver, "message", "Study together?")), 201).path("id").asLong();
    }

    protected Map<String, Object> groupDetails(String name, int capacity) {
        return Map.of("name", name, "description", "Weekly revision", "courseId", course,
                "studyGoals", List.of("CONCEPT_REVIEW"), "preferredStudyMode", "IN_PERSON",
                "maxGroupSize", capacity, "availability", List.of(
                        Map.of("dayOfWeek", "MONDAY", "startTime", "09:00", "endTime", "11:00")));
    }

    protected long group(String leaderToken, int capacity) {
        return expect(call("POST", "/groups", leaderToken, groupDetails("Revision group", capacity)), 201)
                .path("id").asLong();
    }

    protected long apply(long group, String applicantToken) {
        return expect(call("POST", "/groups/" + group + "/join-requests", applicantToken,
                Map.of("message", "May I join?")), 201).path("id").asLong();
    }

    protected Reply approve(long group, long request, String leaderToken) {
        return call("POST", "/groups/" + group + "/join-requests/" + request + "/accept", leaderToken, null);
    }

    protected List<Reply> race(Supplier<Reply> first, Supplier<Reply> second) throws Exception {
        CyclicBarrier start = new CyclicBarrier(2);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var left = executor.submit(() -> { start.await(); return first.get(); });
            var right = executor.submit(() -> { start.await(); return second.get(); });
            return List.of(left.get(25, TimeUnit.SECONDS), right.get(25, TimeUnit.SECONDS));
        }
    }

    protected void statuses(List<Reply> replies, int... expected) {
        assertEquals(java.util.Arrays.stream(expected).boxed().sorted().toList(),
                replies.stream().map(Reply::status).sorted().toList(),
                replies.stream().map(Reply::body).toList().toString());
    }

    protected long count(String sql, Object... args) {
        return database.queryForObject(sql, Long.class, args);
    }

    protected record Reply(int status, JsonNode payload, String body, String contentType) { }
}
