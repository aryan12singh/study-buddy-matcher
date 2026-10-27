package com.studybuddy.music;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;

/**
 * Talks to Spotify's accounts service: swaps a sign-in code for tokens and renews expired ones.
 * The client secret is sent only here, server to server.
 */
@Component
public class SpotifyAccountsClient {
    private final SpotifyProperties properties;
    private final Clock clock;
    private final RestClient client;

    public SpotifyAccountsClient(SpotifyProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        HttpClient http = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(http);
        requests.setReadTimeout(properties.timeout());
        this.client = RestClient.builder().baseUrl(properties.accountsUrl()).requestFactory(requests).build();
    }

    public SpotifyTokens exchangeCode(String code) throws IOException {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", properties.redirectUri());
        return requestTokens(form);
    }

    /** Throws {@link SpotifyNotConnectedException} when Spotify no longer accepts the refresh token. */
    public SpotifyTokens refresh(String refreshToken) throws IOException {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        try {
            return requestTokens(form);
        } catch (IOException failure) {
            if (failure.getCause() instanceof HttpClientErrorException.BadRequest) {
                throw new SpotifyNotConnectedException("Your Spotify connection has expired; connect Spotify again");
            }
            throw failure;
        }
    }

    private SpotifyTokens requestTokens(MultiValueMap<String, String> form) throws IOException {
        String credentials = Base64.getEncoder().encodeToString(
                (properties.clientId() + ":" + properties.clientSecret()).getBytes(StandardCharsets.UTF_8));
        try {
            String reply = client.post()
                    .uri("/api/token")
                    .header("Authorization", "Basic " + credentials)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            return parse(reply, clock);
        } catch (RestClientException failure) {
            throw new IOException("Spotify accounts service unavailable", failure);
        }
    }

    static SpotifyTokens parse(String reply, Clock clock) throws IOException {
        try {
            JsonNode body = reply == null ? null : JsonMapper.builder().build().readTree(reply);
            if (body == null || !body.path("access_token").isString() || !body.path("expires_in").isNumber()) {
                throw new IOException("Spotify accounts service returned no token");
            }
            String refresh = body.path("refresh_token").isString() ? body.path("refresh_token").asString() : null;
            return new SpotifyTokens(body.path("access_token").asString(), refresh,
                    clock.instant().plusSeconds(body.path("expires_in").asLong()));
        } catch (JacksonException invalid) {
            throw new IOException("Spotify accounts service returned an unreadable reply", invalid);
        }
    }
}
