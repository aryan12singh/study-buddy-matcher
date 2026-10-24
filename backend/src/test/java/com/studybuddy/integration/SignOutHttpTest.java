package com.studybuddy.integration;

import org.junit.jupiter.api.Test;

class SignOutHttpTest extends PostgresHttpTest {

    @Test
    void signingOutEndsTheTokenOnTheServer() {
        student("leaving@example.test", "Leaving student", "91234572");
        String token = login("leaving@example.test");
        expect(call("GET", "/auth/me", token, null), 200);

        expect(call("POST", "/auth/logout", token, null), 204);

        expect(call("GET", "/auth/me", token, null), 401);
        expect(call("GET", "/profile/me", token, null), 401);
    }

    @Test
    void signingOutEndsEverySessionOfThatAccountButNoOneElses() {
        student("two-tabs@example.test", "Two tabs", "91234573");
        student("bystander@example.test", "Bystander", "91234574");
        String firstTab = login("two-tabs@example.test");
        String secondTab = login("two-tabs@example.test");
        String bystander = login("bystander@example.test");

        expect(call("POST", "/auth/logout", firstTab, null), 204);

        expect(call("GET", "/auth/me", secondTab, null), 401);
        expect(call("GET", "/auth/me", bystander, null), 200);
        expect(call("GET", "/auth/me", login("two-tabs@example.test"), null), 200);
    }

    @Test
    void administratorsCanSignOutToo() {
        account("admin-out@example.test", "ADMIN");
        String token = login("admin-out@example.test");

        expect(call("POST", "/auth/logout", token, null), 204);

        expect(call("GET", "/admin/users", token, null), 401);
    }

    @Test
    void signingOutWithoutATokenIsUnauthenticated() {
        expect(call("POST", "/auth/logout", null, null), 401);
    }
}
