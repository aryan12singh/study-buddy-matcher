package com.studybuddy.integration;

import com.studybuddy.admin.AdminUserService;
import com.studybuddy.admin.AdminUserUpdateRequest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/** Regression for an email edit overtaking login's initial identity lookup. */
class LoginConcurrencyTest extends PostgresHttpTest {
    @Autowired
    private AdminUserService administrators;

    @Autowired
    private PlatformTransactionManager transactions;

    @Autowired
    private EntityManager entities;

    @Test
    void waitingOldEmailLoginCannotOverwriteCommittedAdminEmailEdit() throws Exception {
        String oldEmail = "before-edit@example.test";
        String newEmail = "after-edit@example.test";
        String runtimePassword = UUID.randomUUID().toString();
        long administrator = account("admin@example.test", "ADMIN");
        long student = student(oldEmail, "Student", "Synthetic contact");
        database.update("update users set password_hash = ? where id = ?", passwords.encode(runtimePassword), student);

        CountDownLatch commitEditor = new CountDownLatch(1);
        CompletableFuture<Integer> editorBackend = new CompletableFuture<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> editing = executor.submit(() -> {
                try {
                    new TransactionTemplate(transactions).executeWithoutResult(status -> {
                        administrators.update(student, new AdminUserUpdateRequest(
                                newEmail, "Student", "SCIS", "Information Systems", 3, null), administrator);
                        entities.flush();
                        editorBackend.complete(database.queryForObject("select pg_backend_pid()", Integer.class));
                        awaitEditorCommit(commitEditor);
                    });
                } catch (RuntimeException | Error error) {
                    editorBackend.completeExceptionally(error);
                    throw error;
                }
            });

            try {
                int editorPid = editorBackend.get(8, TimeUnit.SECONDS);
                Future<Reply> oldLogin = executor.submit(() -> call("POST", "/auth/login", null,
                        Map.of("email", oldEmail, "password", runtimePassword)));
                awaitLoginRowLock(editorPid, oldLogin);
                assertFalse(oldLogin.isDone(), "Old-email login must still be blocked by the editor transaction");

                commitEditor.countDown();
                editing.get(8, TimeUnit.SECONDS);
                Reply rejected = oldLogin.get(8, TimeUnit.SECONDS);
                assertEquals("INVALID_CREDENTIALS", expect(rejected, 401).path("code").asString());
            } finally {
                // Always release the transaction before closing the executor, even when an assertion fails.
                commitEditor.countDown();
                editing.get(8, TimeUnit.SECONDS);
            }
        }

        assertEquals(newEmail, database.queryForObject("select email from users where id = ?", String.class, student));
        assertEquals(0, count("select count(*) from users where id = ? and last_login_at is not null", student),
                "Rejected old-identity login must not update last successful login");
        expect(call("POST", "/auth/login", null, Map.of("email", oldEmail, "password", runtimePassword)), 401);
        var signedIn = expect(call("POST", "/auth/login", null,
                Map.of("email", newEmail, "password", runtimePassword)), 200);
        assertEquals(newEmail, signedIn.path("account").path("email").asString());
        assertEquals(student, signedIn.path("account").path("id").asLong());
        expect(call("GET", "/auth/me", signedIn.path("token").asString(), null), 200);
        assertEquals(newEmail, database.queryForObject("select email from users where id = ?", String.class, student));
        assertEquals(1, count("select count(*) from users where id = ? and last_login_at is not null", student));
    }

    private void awaitLoginRowLock(int editorPid, Future<Reply> login) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
        while (System.nanoTime() < deadline) {
            long blockedLogins = count("""
                    select count(*) from pg_stat_activity
                    where datname = current_database()
                      and wait_event_type = 'Lock'
                      and ? = any(pg_blocking_pids(pid))
                      and lower(query) like '%users%'
                      and lower(query) like '%for %update%'
                    """, editorPid);
            if (blockedLogins > 0) {
                return;
            }
            assertFalse(login.isDone(), "Login finished before the test observed its PostgreSQL row-lock wait");
            Thread.sleep(25);
        }
        fail("PostgreSQL never reported a login row lock blocked by the held editor transaction");
    }

    private static void awaitEditorCommit(CountDownLatch commitEditor) {
        try {
            if (!commitEditor.await(15, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for the test to release the editor transaction");
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Editor transaction interrupted", error);
        }
    }
}
