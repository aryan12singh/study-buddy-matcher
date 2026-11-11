package com.studybuddy.ai;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Which provider is asked first, when the other is asked, and what reaches the feature when neither answers. */
class FallbackAiClientTest {
    private static final Long STUDENT_ID = 7L;
    private static final List<AiAttachment> NOTES = List.of(new AiAttachment("application/pdf", new byte[] {1, 2, 3}));

    /** A provider that always answers or always fails, counting how often it was asked. */
    private static final class FakeProvider implements AiClient {
        private final String reply;
        private final AtomicInteger calls = new AtomicInteger();
        private List<AiAttachment> lastAttachments;

        private FakeProvider(String reply) {
            this.reply = reply;
        }

        @Override
        public String generateJson(Long studentId, String prompt, List<AiAttachment> attachments) {
            calls.incrementAndGet();
            lastAttachments = attachments;
            if (reply == null) {
                throw new AiUnavailableException();
            }
            return reply;
        }
    }

    private static FakeProvider answering(String name) {
        return new FakeProvider("{\"from\":\"" + name + "\"}");
    }

    private static FakeProvider unavailable() {
        return new FakeProvider(null);
    }

    private static AiClient ordered(AiProvider primary, AiClient gemini, AiClient claude) {
        return new FallbackAiClient(primary.inOrder(gemini, claude));
    }

    @Test
    void withGeminiFirstNeverAsksClaudeWhileGeminiAnswers() {
        FakeProvider gemini = answering("gemini");
        FakeProvider claude = answering("claude");

        assertEquals("{\"from\":\"gemini\"}", ordered(AiProvider.GEMINI, gemini, claude).generateJson(STUDENT_ID, "PROMPT"));
        assertEquals(0, claude.calls.get());
    }

    @Test
    void withGeminiFirstAsksClaudeWithTheSamePromptAndFilesWhenGeminiIsUnavailable() {
        FakeProvider gemini = unavailable();
        FakeProvider claude = answering("claude");

        assertEquals("{\"from\":\"claude\"}", ordered(AiProvider.GEMINI, gemini, claude).generateJson(STUDENT_ID, "PROMPT", NOTES));
        assertEquals(1, gemini.calls.get());
        assertEquals(1, claude.calls.get());
        assertEquals(NOTES, claude.lastAttachments);
    }

    @Test
    void withClaudeFirstNeverAsksGeminiWhileClaudeAnswers() {
        FakeProvider gemini = answering("gemini");
        FakeProvider claude = answering("claude");

        assertEquals("{\"from\":\"claude\"}", ordered(AiProvider.CLAUDE, gemini, claude).generateJson(STUDENT_ID, "PROMPT"));
        assertEquals(0, gemini.calls.get());
    }

    @Test
    void withClaudeFirstAsksGeminiWhenClaudeIsUnavailable() {
        FakeProvider gemini = answering("gemini");
        FakeProvider claude = unavailable();

        assertEquals("{\"from\":\"gemini\"}", ordered(AiProvider.CLAUDE, gemini, claude).generateJson(STUDENT_ID, "PROMPT"));
        assertEquals(1, claude.calls.get());
        assertEquals(1, gemini.calls.get());
    }

    @Test
    void reportsTheAiUnavailableWhenNeitherProviderAnswers() {
        FakeProvider gemini = unavailable();
        FakeProvider claude = unavailable();

        assertThrows(AiUnavailableException.class, () -> ordered(AiProvider.GEMINI, gemini, claude).generateJson(STUDENT_ID, "PROMPT"));
        assertEquals(1, gemini.calls.get(), "Each provider is asked once and never retried");
        assertEquals(1, claude.calls.get(), "Each provider is asked once and never retried");
    }

    @Test
    void behavesLikeGeminiAloneWhenClaudeIsSwitchedOffOrHasNoKey() {
        for (ClaudeProperties claude : List.of(claude(false, "unused-key"), claude(true, " "))) {
            for (AiProvider primary : AiProvider.values()) {
                AiProviders providers = new AiProviders(new AiProviderProperties(primary), claude);

                assertEquals("{\"from\":\"gemini\"}", providers.forShortReplies(answering("gemini"), "test").generateJson(STUDENT_ID, "PROMPT"));
                assertEquals("{\"from\":\"gemini\"}", providers.forStudyTools(answering("gemini"), "test").generateJson(STUDENT_ID, "PROMPT"));
                assertThrows(AiUnavailableException.class, () -> providers.forShortReplies(unavailable(), "test").generateJson(STUDENT_ID, "PROMPT"));
            }
        }
    }

    @Test
    void needsAtLeastOneProvider() {
        assertThrows(IllegalArgumentException.class, () -> new FallbackAiClient(List.of()));
    }

    private static ClaudeProperties claude(boolean enabled, String key) {
        return new ClaudeProperties(enabled, "https://api.anthropic.com", key, "test-model", "high",
                8000, 16000, Duration.ofSeconds(1), 10, 10, "UTC", Duration.ofSeconds(60));
    }
}
