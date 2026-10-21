package com.studybuddy.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/** Uses its own low limit; the shared test profile sets a high one so other suites never trip it. */
@TestPropertySource(properties = "app.auth.login-throttle.max-failures=2")
class LoginThrottleHttpTest extends PostgresHttpTest {

    @Test
    void lockedEmailIsRefusedEvenWithTheRightPassword() {
        student("guessed@example.test", "Guessed student", "91234567");
        student("other@example.test", "Other student", "91234568");

        expect(attemptLogin("guessed@example.test", false), 401);
        expect(attemptLogin("guessed@example.test", false), 401);

        var refused = expect(attemptLogin("guessed@example.test", true), 429);
        assertEquals("TOO_MANY_LOGIN_ATTEMPTS", refused.path("code").asString());
        assertEquals("Too many incorrect sign-in attempts. Try again in 15 minutes.", refused.path("message").asString());
        assertFalse(refused.has("token"));
        assertEquals(0, database.queryForObject("select count(*) from users where email='guessed@example.test' and last_login_at is not null", Integer.class));
        expect(attemptLogin("other@example.test", true), 200);
    }

    @Test
    void unknownEmailIsThrottledTheSameWaySoItRevealsNothing() {
        expect(attemptLogin("nobody@example.test", false), 401);
        expect(attemptLogin("nobody@example.test", false), 401);

        assertEquals("TOO_MANY_LOGIN_ATTEMPTS", expect(attemptLogin("nobody@example.test", false), 429).path("code").asString());
    }

    @Test
    void emailIsThrottledAfterNormalization() {
        student("mixed@example.test", "Mixed case", "91234569");
        expect(call("POST", "/auth/login", null, Map.of("email", " MIXED@example.test ", "password", "not-the-password")), 401);
        expect(attemptLogin("mixed@example.test", false), 401);

        expect(attemptLogin("mixed@example.test", true), 429);
    }
}
