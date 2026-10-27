package com.studybuddy.music;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Spotify sign-in and playback settings, bound from {@code app.spotify.*}. The client ID and secret
 * come only from the git-ignored {@code application-local.yml}; without them Spotify stays off.
 */
@Validated
@ConfigurationProperties("app.spotify")
public record SpotifyProperties(
        String clientId,
        String clientSecret,
        @NotBlank String redirectUri,
        @NotBlank String returnUrl,
        @NotBlank String accountsUrl,
        @NotBlank String apiUrl,
        @NotEmpty List<String> scopes,
        @NotNull Duration timeout,
        @NotNull Duration authorizationLifetime,
        @NotNull Duration tokenRefreshMargin,
        // Spotify's search returns at most 10 results per request since February 2026.
        @Positive @Max(10) int searchLimit,
        @Positive int maxQueryLength,
        // Spotify IDs are base-62; the frontend puts this into an embed URL.
        @NotBlank @Pattern(regexp = "[A-Za-z0-9]+") String embedPlaylistId) {

    public SpotifyProperties {
        // A zero timeout makes the HTTP client fail at startup with an unclear error.
        if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
            throw new IllegalArgumentException("app.spotify.timeout must be positive");
        }
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
    }

    /** True only when both app credentials are configured. */
    public boolean ready() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    /** Never print the secret, even in a startup error. */
    @Override
    public String toString() {
        return "SpotifyProperties[clientId=" + (clientId == null ? "absent" : "set") + ", clientSecret="
                + (clientSecret == null ? "absent" : "set") + ", redirectUri=" + redirectUri + "]";
    }
}
