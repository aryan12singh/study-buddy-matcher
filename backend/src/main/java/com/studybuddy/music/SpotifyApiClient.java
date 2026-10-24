package com.studybuddy.music;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Calls the Spotify Web API on behalf of a signed-in student, using their access token. */
@Component
public class SpotifyApiClient {
    private final SpotifyProperties properties;
    private final RestClient client;

    public SpotifyApiClient(SpotifyProperties properties) {
        this.properties = properties;
        HttpClient http = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(http);
        requests.setReadTimeout(properties.timeout());
        this.client = RestClient.builder().baseUrl(properties.apiUrl()).requestFactory(requests).build();
    }

    public List<SpotifyPlaylist> searchPlaylists(String accessToken, String query) throws IOException {
        try {
            String reply = client.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("q", query)
                            .queryParam("type", "playlist")
                            .queryParam("limit", properties.searchLimit())
                            .build())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);
            return parsePlaylists(reply);
        } catch (RestClientException failure) {
            throw new IOException("Spotify search unavailable", failure);
        }
    }

    /** Starts a playlist on the student's player in this app, identified by its device ID. */
    public void playPlaylist(String accessToken, String deviceId, String playlistUri) throws IOException {
        try {
            client.put()
                    .uri(uri -> uri.path("/me/player/play").queryParam("device_id", deviceId).build())
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("context_uri", playlistUri))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound noDevice) {
            throw new SpotifyPlaybackException("The player in this tab is not ready; wait a moment and try again");
        } catch (HttpClientErrorException.Forbidden notPremium) {
            throw new SpotifyPlaybackException("Spotify only allows playback here with a Premium account");
        } catch (RestClientException failure) {
            throw new IOException("Spotify playback unavailable", failure);
        }
    }

    static List<SpotifyPlaylist> parsePlaylists(String reply) throws IOException {
        if (reply == null) {
            throw new IOException("Spotify search returned no reply");
        }
        try {
            List<SpotifyPlaylist> playlists = new ArrayList<>();
            for (JsonNode item : JsonMapper.builder().build().readTree(reply).path("playlists").path("items")) {
                // Spotify returns null entries for playlists that are no longer available.
                if (item.isObject() && item.path("uri").isString()) {
                    String image = item.path("images").path(0).path("url").asString("");
                    playlists.add(new SpotifyPlaylist(item.path("id").asString(""), item.path("uri").asString(),
                            item.path("name").asString(""), item.path("owner").path("display_name").asString(""),
                            image.isBlank() ? null : image));
                }
            }
            return playlists;
        } catch (JacksonException invalid) {
            throw new IOException("Spotify search returned an unreadable reply", invalid);
        }
    }
}
