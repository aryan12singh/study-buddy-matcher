package com.studybuddy.music;

import java.time.Duration;
import java.util.List;

/** Spotify settings shared by the tests; only the credentials vary. */
final class SpotifyTestSettings {
    private SpotifyTestSettings() {
    }

    static SpotifyProperties withCredentials(String clientId, String clientSecret) {
        return new SpotifyProperties(clientId, clientSecret, "http://127.0.0.1:8080/api/spotify/callback",
                "http://localhost:5173/study-room", "https://accounts.spotify.com",
                List.of("streaming", "user-read-email"), Duration.ofSeconds(5), Duration.ofMinutes(10),
                Duration.ofSeconds(60), "3bZTQGrvewbkJJe6UpTFKc");
    }

    static SpotifyProperties configured() {
        return withCredentials("client", "secret");
    }
}
