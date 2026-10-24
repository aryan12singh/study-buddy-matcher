package com.studybuddy.matchexplanation;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiExplanationProviderTest {
    private static final Long STUDENT_ID = 7L;
    private static final String REPLY = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"explanation\\\":\\\"You both take IS442.\\\"}\"}]}}]}";

    private HttpServer server;
    private ExplanationQuota quota;
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> requestKey = new AtomicReference<>();
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private volatile int status = 200;
    private volatile long delayMillis = 0;
    private volatile String reply = REPLY;

    @BeforeEach
    void startStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            requestKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = reply.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    @AfterEach
    void stopStub() {
        server.stop(0);
    }

    private GeminiExplanationProvider provider() {
        return provider("");
    }

    /** The real base URL carries a version path, as Gemini's does. */
    private GeminiExplanationProvider provider(String basePath) {
        AiExplanationProperties properties = new AiExplanationProperties(true,
                "http://127.0.0.1:" + server.getAddress().getPort() + basePath, "test-model", "test-key",
                Duration.ofMillis(500), 200, 600, 10, 10, "UTC", Duration.ofSeconds(60));
        quota = new ExplanationQuota(properties);
        return new GeminiExplanationProvider(properties, ExplanationFixtures.JSON, quota);
    }

    @Test
    void keepsTheVersionPathOfTheBaseUrl() {
        assertTrue(provider("/v1beta").explain("PROMPT").isPresent());

        assertEquals("/v1beta/models/test-model:generateContent", requestPath.get());
    }

    @Test
    void returnsTheModelsTextAndSendsOnlyThePromptAndSettings() {
        assertEquals(Optional.of("{\"explanation\":\"You both take IS442.\"}"), provider().explain("PROMPT-TEXT"));

        assertEquals("/models/test-model:generateContent", requestPath.get());
        assertEquals("test-key", requestKey.get());
        String body = requestBody.get();
        assertTrue(body.contains("PROMPT-TEXT"), body);
        assertTrue(body.contains("\"responseMimeType\":\"application/json\""), body);
        assertTrue(body.contains("\"maxOutputTokens\":200"), body);
        assertTrue(body.contains("\"thinkingBudget\":0"), body);
        assertFalse(body.contains("test-key"), body);
    }

    @Test
    void rateLimitReturnsEmptyAndPausesFurtherCalls() {
        status = 429;

        assertTrue(provider().explain("PROMPT").isEmpty());
        assertFalse(quota.tryAcquire(STUDENT_ID));
    }

    @Test
    void serverErrorReturnsEmpty() {
        status = 500;
        assertTrue(provider().explain("PROMPT").isEmpty());
    }

    @Test
    void slowReplyReturnsEmpty() {
        delayMillis = 1500;
        assertTrue(provider().explain("PROMPT").isEmpty());
    }

    @Test
    void replyWithoutCandidatesReturnsEmpty() {
        reply = "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}";
        assertTrue(provider().explain("PROMPT").isEmpty());
    }

    @Test
    void unreadableReplyReturnsEmpty() {
        reply = "not json";
        assertTrue(provider().explain("PROMPT").isEmpty());
    }
}
