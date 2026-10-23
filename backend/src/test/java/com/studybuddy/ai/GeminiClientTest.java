package com.studybuddy.ai;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiClientTest {
    private static final Long STUDENT_ID = 7L;
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String REPLY = """
            {"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"{\\"topics\\":[\\"Inheritance\\"]}"}]}}]}""";

    private HttpServer server;
    private AiQuota quota;
    private final AtomicInteger calls = new AtomicInteger();
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
            calls.incrementAndGet();
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

    private GeminiClient client() {
        return client(true, "", 10);
    }

    /** The real base URL carries a version path, as Gemini's does. */
    private GeminiClient client(boolean enabled, String basePath, int dailyLimit) {
        AiAssistantProperties properties = new AiAssistantProperties(enabled,
                "http://127.0.0.1:" + server.getAddress().getPort() + basePath, "test-model", "test-key",
                Duration.ofMillis(500), 4000, dailyLimit, dailyLimit, "UTC", Duration.ofSeconds(60));
        quota = new AiQuota(properties);
        return new GeminiClient(properties, JSON, quota);
    }

    @Test
    void keepsTheVersionPathOfTheBaseUrl() {
        client(true, "/v1beta", 10).generateJson(STUDENT_ID, "PROMPT");

        assertEquals("/v1beta/models/test-model:generateContent", requestPath.get());
    }

    @Test
    void returnsTheModelsTextAndSendsOnlyThePromptAndSettings() {
        assertEquals("{\"topics\":[\"Inheritance\"]}", client().generateJson(STUDENT_ID, "PROMPT-TEXT"));

        assertEquals("test-key", requestKey.get());
        String body = requestBody.get();
        assertTrue(body.contains("PROMPT-TEXT"), body);
        assertTrue(body.contains("\"responseMimeType\":\"application/json\""), body);
        assertTrue(body.contains("\"maxOutputTokens\":4000"), body);
        assertTrue(body.contains("\"thinkingBudget\":0"), body);
        assertFalse(body.contains("test-key"), body);
        assertFalse(body.contains("inlineData"), body);
    }

    @Test
    void sendsAnAttachedFileInlineBeforeThePrompt() {
        byte[] notes = "Week 3: polymorphism".getBytes(StandardCharsets.UTF_8);

        client().generateJson(STUDENT_ID, "PROMPT-TEXT", List.of(new AiAttachment("text/plain", notes)));

        String body = requestBody.get();
        assertTrue(body.contains("\"mimeType\":\"text/plain\""), body);
        int file = body.indexOf(Base64.getEncoder().encodeToString(notes));
        assertTrue(file >= 0, body);
        assertTrue(file < body.indexOf("PROMPT-TEXT"), body);
    }

    @Test
    void joinsTextPartsAndSkipsThoughts() {
        reply = """
                {"candidates":[{"finishReason":"STOP","content":{"parts":[
                {"text":"thinking aloud","thought":true},{"text":"{\\"a\\":"},{"text":"1}"}]}}]}""";

        assertEquals("{\"a\":1}", client().generateJson(STUDENT_ID, "PROMPT"));
    }

    @Test
    void switchedOffNeverCallsGeminiOrSpendsTheAllowance() {
        GeminiClient client = client(false, "", 1);

        assertThrows(AiUnavailableException.class, () -> client.generateJson(STUDENT_ID, "PROMPT"));
        assertEquals(0, calls.get());
        assertTrue(quota.tryAcquire(STUDENT_ID));
    }

    @Test
    void anExhaustedAllowanceNeverCallsGemini() {
        GeminiClient client = client(true, "", 1);
        client.generateJson(STUDENT_ID, "PROMPT");

        assertThrows(AiUnavailableException.class, () -> client.generateJson(STUDENT_ID, "PROMPT"));
        assertEquals(1, calls.get());
    }

    @Test
    void rateLimitThrowsAndPausesFurtherCalls() {
        status = 429;
        GeminiClient client = client();

        assertThrows(AiUnavailableException.class, () -> client.generateJson(STUDENT_ID, "PROMPT"));
        assertFalse(quota.tryAcquire(STUDENT_ID));
    }

    @Test
    void serverErrorThrows() {
        status = 500;
        GeminiClient client = client();
        assertThrows(AiUnavailableException.class, () -> client.generateJson(STUDENT_ID, "PROMPT"));
    }

    @Test
    void slowReplyThrows() {
        delayMillis = 1500;
        GeminiClient client = client();
        assertThrows(AiUnavailableException.class, () -> client.generateJson(STUDENT_ID, "PROMPT"));
    }

    @Test
    void replyCutOffByTheTokenLimitThrows() {
        reply = """
                {"candidates":[{"finishReason":"MAX_TOKENS","content":{"parts":[{"text":"{\\"topics\\":[\\"Inh"}]}}]}""";
        GeminiClient client = client();
        assertThrows(AiUnavailableException.class, () -> client.generateJson(STUDENT_ID, "PROMPT"));
    }

    @Test
    void replyWithoutCandidatesThrows() {
        reply = "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}";
        GeminiClient client = client();
        assertThrows(AiUnavailableException.class, () -> client.generateJson(STUDENT_ID, "PROMPT"));
    }

    @Test
    void blankReplyThrows() {
        reply = "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"  \"}]}}]}";
        GeminiClient client = client();
        assertThrows(AiUnavailableException.class, () -> client.generateJson(STUDENT_ID, "PROMPT"));
    }

    @Test
    void unreadableReplyThrows() {
        reply = "not json";
        GeminiClient client = client();
        assertThrows(AiUnavailableException.class, () -> client.generateJson(STUDENT_ID, "PROMPT"));
    }
}
