package com.studybuddy.music;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;

/**
 * Calls Freesound's search. One request returns the preview URLs too, so no second lookup per
 * sound is needed. The key travels in the Authorization header, never in the URL, so it stays out
 * of request logs.
 */
@Component
public class FreesoundClient implements SoundSearchClient {
    /** Only the fields the mixer shows; Freesound returns far more by default. */
    private static final String FIELDS = "id,name,username,license,previews";
    private static final String PREVIEW_FIELD = "preview-hq-mp3";

    private final AmbientProperties properties;
    private final RestClient client;

    public FreesoundClient(AmbientProperties properties) {
        this.properties = properties;
        HttpClient http = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(http);
        requests.setReadTimeout(properties.timeout());
        this.client = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(requests).build();
    }

    @Override
    public List<AmbientSound> search(String query) throws IOException {
        try {
            String reply = client.get()
                    .uri(uri -> uri.path("/search/")
                            .queryParam("query", query)
                            .queryParam("filter", filter(properties))
                            .queryParam("fields", FIELDS)
                            .queryParam("page_size", properties.candidateCount())
                            .build())
                    .header("Authorization", "Token " + properties.apiKey())
                    .retrieve()
                    .body(String.class);
            return parse(reply);
        } catch (RestClientException failure) {
            throw new IOException("Sound provider unavailable", failure);
        }
    }

    /**
     * Very short clips loop audibly, so only sounds within the configured length are asked for,
     * and only under the configured licence, so the app need not credit each author on screen.
     */
    static String filter(AmbientProperties properties) {
        return "duration:[" + properties.minDurationSeconds() + " TO " + properties.maxDurationSeconds() + "]"
                + " license:\"" + properties.license() + "\"";
    }

    static List<AmbientSound> parse(String reply) throws IOException {
        if (reply == null) {
            throw new IOException("Sound provider returned no reply");
        }
        try {
            List<AmbientSound> sounds = new ArrayList<>();
            for (JsonNode result : JsonMapper.builder().build().readTree(reply).path("results")) {
                String preview = result.path("previews").path(PREVIEW_FIELD).asString("");
                // A sound without a preview cannot be played, so it is left out.
                if (!preview.isBlank()) {
                    sounds.add(new AmbientSound(result.path("id").asLong(), result.path("name").asString(""),
                            result.path("username").asString(""), result.path("license").asString(""), preview));
                }
            }
            return sounds;
        } catch (JacksonException invalid) {
            throw new IOException("Sound provider returned an unreadable reply", invalid);
        }
    }
}
