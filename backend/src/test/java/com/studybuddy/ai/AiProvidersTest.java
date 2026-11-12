package com.studybuddy.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.errors.AnthropicIoException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Every AI feature sends its Claude calls through the one connection the application built. */
class AiProvidersTest {
    private static final Long STUDENT_ID = 7L;
    private static final ClaudeProperties CLAUDE_ON = new ClaudeProperties(true, "https://api.anthropic.com", "test-key",
            "test-model", "high", 8000, 16000, Duration.ofSeconds(1), 10, 10, "UTC", Duration.ofSeconds(60));
    private static final AiClient GEMINI_DOWN = (studentId, prompt, attachments) -> {
        throw new AiUnavailableException();
    };

    @Test
    void theExplanationPixelAndStudyToolClientsShareOneConnection() {
        AnthropicClient connection = mock(AnthropicClient.class);
        when(connection.messages()).thenThrow(new AnthropicIoException("stub: no network in this test"));
        AiProviders providers = new AiProviders(new AiProviderProperties(AiProvider.CLAUDE), CLAUDE_ON, Optional.of(connection));
        List<AiClient> features = List.of(providers.forShortReplies(GEMINI_DOWN, "explanation"),
                providers.forShortReplies(GEMINI_DOWN, "assistant chat"), providers.forStudyTools(GEMINI_DOWN, "assistant"));

        for (AiClient feature : features) {
            assertThrows(AiUnavailableException.class, () -> feature.generateJson(STUDENT_ID, "PROMPT"));
        }

        verify(connection, times(features.size())).messages();
        verify(connection, never()).close();
    }
}
