package com.studybuddy.ai;

import com.anthropic.client.AnthropicClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Claude backup client against a stub server: what it sends, what it accepts and how each failure ends. */
class ClaudeClientTest {
    private static final Long STUDENT_ID = 7L;

    private HttpServer server;
    private AiQuota quota;
    private AnthropicClient connection;
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> requestKey = new AtomicReference<>();
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String reply = message("end_turn", "{\\\"topics\\\":[\\\"Inheritance\\\"]}");

    /** A Messages API reply with one text block. */
    private static String message(String stopReason, String escapedText) {
        return """
                {"id":"msg_1","type":"message","role":"assistant","model":"test-model",
                 "content":[{"type":"text","text":"%s"}],"stop_reason":"%s","stop_sequence":null,
                 "usage":{"input_tokens":10,"output_tokens":5}}""".formatted(escapedText, stopReason);
    }

    @BeforeEach
    void startStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            requestPath.set(exchange.getRequestURI().getPath());
            requestKey.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
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
        if (connection != null) {
            connection.close();
        }
        server.stop(0);
    }

    private ClaudeClient client() {
        return client(true, "test-key", 10);
    }

    private ClaudeClient client(boolean enabled, String key, int dailyLimit) {
        ClaudeProperties properties = new ClaudeProperties(enabled, "http://127.0.0.1:" + server.getAddress().getPort(), key,
                "test-model", "high", 8000, 16000, Duration.ofSeconds(2), dailyLimit, dailyLimit, "UTC", Duration.ofSeconds(60));
        quota = new AiQuota(dailyLimit, dailyLimit, Duration.ofSeconds(60), ZoneId.of("UTC"));
        // As the application does: a connection only when Claude is switched on with a key.
        connection = properties.ready() ? new ClaudeConfiguration().anthropicClient(properties) : null;
        return new ClaudeClient(properties, "test", properties.maxTokens(), quota, Optional.ofNullable(connection));
    }

    @Test
    void returnsTheModelsTextAndSendsThePromptModelEffortAndReplySize() {
        assertEquals("{\"topics\":[\"Inheritance\"]}", client().generateJson(STUDENT_ID, "PROMPT-TEXT"));

        assertEquals("/v1/messages", requestPath.get());
        assertEquals("Bearer test-key", requestKey.get(), "The SDK sends the key in a header, never in the body or URL");
        String body = requestBody.get();
        assertTrue(body.contains("PROMPT-TEXT"), body);
        assertTrue(body.contains("\"model\":\"test-model\""), body);
        assertTrue(body.contains("\"max_tokens\":8000"), body);
        assertTrue(body.contains("\"effort\":\"high\""), body);
        assertFalse(body.contains("thinking"), "No thinking setting is sent; the model thinks adaptively");
        assertFalse(body.contains("temperature"), body);
        assertFalse(body.contains("test-key"), body);
    }

    @Test
    void sendsAnAttachedPdfAsADocumentBeforeThePrompt() {
        byte[] pdf = "%PDF-1.4 notes".getBytes(StandardCharsets.UTF_8);

        client().generateJson(STUDENT_ID, "PROMPT-TEXT", List.of(new AiAttachment("application/pdf", pdf)));

        String body = requestBody.get();
        assertTrue(body.contains("\"type\":\"document\""), body);
        assertTrue(body.contains("\"media_type\":\"application/pdf\""), body);
        assertTrue(body.contains(Base64.getEncoder().encodeToString(pdf)), body);
        assertTrue(body.indexOf("\"type\":\"document\"") < body.indexOf("PROMPT-TEXT"), "The file goes before the instructions");
    }

    @Test
    void unwrapsJsonThatClaudePutInACodeFence() {
        reply = message("end_turn", "```json\\n{\\\"intent\\\":\\\"UNKNOWN\\\"}\\n```");

        assertEquals("{\"intent\":\"UNKNOWN\"}", client().generateJson(STUDENT_ID, "PROMPT"));
    }

    @Test
    void treatsARefusalAsUnavailable() {
        reply = message("refusal", "");

        assertThrows(AiUnavailableException.class, () -> client().generateJson(STUDENT_ID, "PROMPT"));
    }

    @Test
    void treatsAReplyCutOffAtTheTokenLimitAsUnavailable() {
        reply = message("max_tokens", "{\\\"topics\\\":[\\\"Inher");

        assertThrows(AiUnavailableException.class, () -> client().generateJson(STUDENT_ID, "PROMPT"));
    }

    @Test
    void treatsAServerErrorAsUnavailableWithoutRetrying() {
        status = 503;
        reply = "{\"type\":\"error\",\"error\":{\"type\":\"overloaded_error\",\"message\":\"Overloaded\"}}";

        assertThrows(AiUnavailableException.class, () -> client().generateJson(STUDENT_ID, "PROMPT"));
        assertEquals(1, calls.get());
    }

    @Test
    void pausesAfterARateLimitSoTheNextCallIsNotSent() {
        status = 429;
        reply = "{\"type\":\"error\",\"error\":{\"type\":\"rate_limit_error\",\"message\":\"Slow down\"}}";
        ClaudeClient client = client();

        assertThrows(AiUnavailableException.class, () -> client.generateJson(STUDENT_ID, "PROMPT"));
        assertThrows(AiUnavailableException.class, () -> client.generateJson(STUDENT_ID, "PROMPT"));
        assertEquals(1, calls.get());
    }

    @Test
    void refusesCallsOverTheDailyAllowanceWithoutCallingClaude() {
        ClaudeClient client = client(true, "test-key", 1);
        client.generateJson(STUDENT_ID, "PROMPT");

        assertThrows(AiUnavailableException.class, () -> client.generateJson(STUDENT_ID, "PROMPT"));
        assertEquals(1, calls.get());
    }

    @Test
    void neverCallsClaudeWhenSwitchedOffOrWithoutAKey() {
        assertThrows(AiUnavailableException.class, () -> client(false, "test-key", 10).generateJson(STUDENT_ID, "PROMPT"));
        assertThrows(AiUnavailableException.class, () -> client(true, null, 10).generateJson(STUDENT_ID, "PROMPT"));
        assertEquals(0, calls.get());
        assertTrue(quota.tryAcquire(STUDENT_ID), "A switched-off backup spends no allowance");
    }

    @Test
    void neverPrintsTheKey() {
        ClaudeProperties properties = new ClaudeProperties(true, "https://api.anthropic.com", "secret-key", "test-model", "high",
                8000, 16000, Duration.ofSeconds(2), 10, 10, "UTC", Duration.ofSeconds(60));

        assertFalse(properties.toString().contains("secret-key"));
    }
}
