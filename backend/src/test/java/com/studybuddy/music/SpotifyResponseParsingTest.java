package com.studybuddy.music;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class SpotifyResponseParsingTest {
    private final Instant now = Instant.parse("2026-10-22T08:00:00Z");

    @Test void tokenReplyExpiresAfterTheGivenSeconds() throws Exception {
        var tokens = SpotifyAccountsClient.parse("{\"access_token\":\"a\",\"refresh_token\":\"r\",\"expires_in\":3600}",
                Clock.fixed(now, ZoneOffset.UTC));
        assertEquals(new SpotifyTokens("a", "r", now.plusSeconds(3600)), tokens);
    }

    @Test void tokenReplyWithoutAccessTokenIsAFailure() {
        assertThrows(IOException.class, () -> SpotifyAccountsClient.parse("{\"error\":\"invalid_grant\"}", Clock.systemUTC()));
        assertThrows(IOException.class, () -> SpotifyAccountsClient.parse(null, Clock.systemUTC()));
    }

    @Test void tokensNeverAppearInText() {
        assertFalse(new SpotifyTokens("secret-access", "secret-refresh", now).toString().contains("secret"));
        assertFalse(SpotifyTestSettings.withCredentials("client", "top-secret-value").toString().contains("top-secret-value"));
    }
}
