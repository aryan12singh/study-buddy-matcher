package com.studybuddy.matchexplanation;

import com.studybuddy.ai.GeminiSettings;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** E1's own AI settings: readiness, key privacy and the Gemini client settings built from them. */
class AiExplanationPropertiesTest {

    private static AiExplanationProperties withKey(boolean enabled, String apiKey) {
        return new AiExplanationProperties(enabled, "http://localhost/v1beta", "e1-model", apiKey, Duration.ofSeconds(4),
                200, 600, 1, 1, "UTC", Duration.ofSeconds(60));
    }

    @Test
    void isReadyOnlyWhenEnabledWithAKey() {
        assertTrue(withKey(true, "key").ready());
        assertFalse(withKey(false, "key").ready());
        assertFalse(withKey(true, null).ready());
        assertFalse(withKey(true, "  ").ready());
    }

    @Test
    void neverPrintsTheKey() {
        assertFalse(withKey(true, "secret-key").toString().contains("secret-key"));
    }

    @Test
    void rejectsATimeoutThatIsNotPositive() {
        assertThrows(IllegalArgumentException.class, () -> new AiExplanationProperties(true, "u", "m", "key",
                Duration.ZERO, 1, 1, 1, 1, "UTC", Duration.ofSeconds(60)));
    }

    @Test
    void geminiSettingsUseE1sOwnKeyModelTimeoutAndReplySize() {
        assertEquals(new GeminiSettings("explanation", true, "http://localhost/v1beta", "e1-model", "e1-key",
                Duration.ofSeconds(4), 200, true), withKey(true, "e1-key").geminiSettings("explanation", true));
    }
}
