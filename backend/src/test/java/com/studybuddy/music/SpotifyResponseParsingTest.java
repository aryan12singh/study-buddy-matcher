package com.studybuddy.music;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

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

    @Test void playlistSearchSkipsUnavailableEntries() throws Exception {
        String reply = """
                {"playlists": {"items": [
                  {"id": "p1", "uri": "spotify:playlist:p1", "name": "Lofi", "owner": {"display_name": "Spotify"},
                   "images": [{"url": "https://img/p1"}]},
                  null,
                  {"id": "p2", "uri": "spotify:playlist:p2", "name": "Rain", "owner": {"display_name": "ana"}, "images": []}
                ]}}""";
        assertEquals(List.of(
                new SpotifyPlaylist("p1", "spotify:playlist:p1", "Lofi", "Spotify", "https://img/p1"),
                new SpotifyPlaylist("p2", "spotify:playlist:p2", "Rain", "ana", null)), SpotifyApiClient.parsePlaylists(reply));
    }

    @Test void tokensNeverAppearInText() {
        assertFalse(new SpotifyTokens("secret-access", "secret-refresh", now).toString().contains("secret"));
        assertFalse(SpotifyTestSettings.withCredentials("client", "top-secret-value").toString().contains("top-secret-value"));
    }
}
