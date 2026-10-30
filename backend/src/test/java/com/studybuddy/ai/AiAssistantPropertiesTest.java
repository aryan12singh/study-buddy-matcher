package com.studybuddy.ai;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The study tools' AI settings: readiness, key privacy, and the client settings and allowance built from them. */
class AiAssistantPropertiesTest {

    private static AiAssistantProperties withKey(boolean enabled, String apiKey) {
        return new AiAssistantProperties(enabled, "http://localhost/v1beta", "full-model", apiKey, Duration.ofSeconds(9),
                4000, 1, 1, "UTC", Duration.ofSeconds(60));
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
        assertFalse(withKey(true, "secret-key").geminiSettings("assistant", true).toString().contains("secret-key"));
    }

    @Test
    void rejectsATimeoutThatIsNotPositive() {
        assertThrows(IllegalArgumentException.class, () -> new AiAssistantProperties(true, "u", "m", "key",
                Duration.ZERO, 1, 1, 1, "UTC", Duration.ofSeconds(60)));
    }

    @Test
    void geminiSettingsCarryTheKeyModelTimeoutAndReplySize() {
        assertEquals(new GeminiSettings("assistant", true, "http://localhost/v1beta", "full-model", "key",
                Duration.ofSeconds(9), 4000, true), withKey(true, "key").geminiSettings("assistant", true));
    }

    @Test
    void eachQuotaCountsSeparately() {
        AiAssistantProperties properties = withKey(true, "key");
        AiQuota first = properties.newQuota();

        assertTrue(first.tryAcquire(7L));
        assertFalse(first.tryAcquire(7L));
        assertTrue(properties.newQuota().tryAcquire(7L));
    }
}
